/**
 * Jenkins DSL for Steeltoe Samples
 */

samplePaths = [
    'Configuration/src/AspDotNetCore/CloudFoundry',
    'Configuration/src/AspDotNetCore/Simple',
    'Configuration/src/AspDotNetCore/SimpleCloudFoundry',
    'Connectors/src/AspDotNetCore/MySql',
    'Connectors/src/AspDotNetCore/MySqlEFCore',
    'Connectors/src/AspDotNetCore/MySqlEF6',
    'Connectors/src/AspDotNetCore/PostgreSql',
    'Connectors/src/AspDotNetCore/PostgreEFCore',
    'Connectors/src/AspDotNetCore/RabbitMQ',
    'Connectors/src/AspDotNetCore/Redis',
    'Security/src/AspDotNetCore/CloudFoundrySingleSignon',
    'Management/src/AspDotNetCore/CloudFoundry',
]

platforms = [
    'ubuntu1604',
    'win2012',
]

alertees = [
    'ccheetham',
    'dtillman',
    'jkonicki',
    'hsarella',
    'thess',
]

def jobForSample(def sample, def platform) {
    "steeltoe-samples-${sample.split('/').findAll { !(it in ['src']) }.collect { it.toLowerCase() }.join('-')}-${platform}"
}

def descriptionForSample(def sample, def platform) {
    nodes = sample.split('/')
    library = nodes[0]
    sample = nodes[-1]
    switch (platform) {
        case ~/win.*/:
            os = 'Windows'
            break
        case ~/ubuntu.*/:
            os = 'Linux'
            break
        default:
            os = platform
            break
    }
    switch (nodes[-2]) {
        case ~/.*NetCore$/:
            dotnet = '.NET Core'
            break
        case ~/.*Net4/:
            dotnet = '.NET Framework 4'
            break
        default:
            dotnet = nodes[-2]
            break
    }
    "SteeltoeOSS ${os} Sample CI Build for ${library} ${sample} for ${dotnet}"
}

samplePaths.each { samplePath ->
    platforms.each { platform ->
        job(jobForSample(samplePath, platform)) {
            description(descriptionForSample(samplePath, platform))
            wrappers {
                credentialsBinding {
                    usernamePassword('STEELTOE_PCF_CREDENTIALS', 'steeltoe-pcf')
                }
                preBuildCleanup()
            }
            label("steeltoe && ${platform}")
            scm {
                git {
                    remote {
                        github('SteeltoeOSS/Samples', 'https')
                        branch('dev')
                    }
                    configure { gitScm ->
                        gitScm / 'extensions' << 'hudson.plugins.git.extensions.impl.PathRestriction' {
                            includedRegions([
                                    "${samplePath}/.*",
                                    "behave.ini",
                                    "ci/.*",
                                    "config/.*",
                                    "environment.py",
                                    "pyenv.pkgs",
                                    "pylib/*",
                                    "test-run.*",
                                    "test-setup.*",
                                    ].join('\n'))
                        }
                    }
                }
            }
            triggers {
                scm('H/15 * * * *')
            }
            steps {
                if (platform.startsWith('win')) {
                    batchFile("ci\\jenkins.cmd ${samplePath.replaceAll('/', '\\\\')}")
                } else {
                    shell("ci/jenkins.sh ${samplePath}")
                }
            }
            publishers {
                archiveArtifacts('test.log')
                archiveJunit('test.out/reports/*.xml')
                mailer(alertees.collect { "${it}@pivotal.io" }.join(' '), true, false)
            }
            logRotator {
                numToKeep(5)
            }
        }
    }
}

// vim: et sw=4 sts=4
