param([string]$MavenRepository = (Join-Path $env:USERPROFILE '.m2/repository'))
$ErrorActionPreference = 'Stop'
$qaRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$backendRoot = (Resolve-Path (Join-Path $qaRoot '..')).Path
$outputRoot = Join-Path $qaRoot '.verification'
$libraryRoot = Join-Path $outputRoot 'lib'
$classRoot = Join-Path $outputRoot 'classes'
New-Item -ItemType Directory -Force -Path $libraryRoot, $classRoot | Out-Null

# Reuse the project's cached dependencies; do not download or alter the shared POM.
$jars = @(
    'org/hibernate/orm/hibernate-core/6.6.53.Final/hibernate-core-6.6.53.Final.jar'
    'jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar'
    'jakarta/transaction/jakarta.transaction-api/2.0.1/jakarta.transaction-api-2.0.1.jar'
    'org/jboss/logging/jboss-logging/3.6.3.Final/jboss-logging-3.6.3.Final.jar'
    'org/hibernate/common/hibernate-commons-annotations/7.0.3.Final/hibernate-commons-annotations-7.0.3.Final.jar'
    'io/smallrye/jandex/3.2.0/jandex-3.2.0.jar'
    'com/fasterxml/classmate/1.5.1/classmate-1.5.1.jar'
    'net/bytebuddy/byte-buddy/1.17.8/byte-buddy-1.17.8.jar'
    'jakarta/xml/bind/jakarta.xml.bind-api/4.0.0/jakarta.xml.bind-api-4.0.0.jar'
    'org/glassfish/jaxb/jaxb-runtime/4.0.2/jaxb-runtime-4.0.2.jar'
    'org/glassfish/jaxb/jaxb-core/4.0.2/jaxb-core-4.0.2.jar'
    'org/glassfish/jaxb/txw2/4.0.2/txw2-4.0.2.jar'
    'jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar'
    'org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar'
    'com/h2database/h2/2.3.232/h2-2.3.232.jar'
    'jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2.jar'
    'jakarta/servlet/jakarta.servlet-api/6.0.0/jakarta.servlet-api-6.0.0.jar'
    'com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar'
    'com/fasterxml/jackson/core/jackson-core/2.21.4/jackson-core-2.21.4.jar'
    'com/fasterxml/jackson/core/jackson-databind/2.21.4/jackson-databind-2.21.4.jar'
    'io/swagger/core/v3/swagger-annotations-jakarta/2.2.29/swagger-annotations-jakarta-2.2.29.jar'
    'org/slf4j/slf4j-api/2.0.18/slf4j-api-2.0.18.jar'
    'org/junit/platform/junit-platform-launcher/1.12.2/junit-platform-launcher-1.12.2.jar'
    'org/junit/platform/junit-platform-engine/1.12.2/junit-platform-engine-1.12.2.jar'
    'org/junit/platform/junit-platform-commons/1.12.2/junit-platform-commons-1.12.2.jar'
    'org/junit/jupiter/junit-jupiter-api/5.12.2/junit-jupiter-api-5.12.2.jar'
    'org/junit/jupiter/junit-jupiter-engine/5.12.2/junit-jupiter-engine-5.12.2.jar'
    'org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar'
    'org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar'
    'org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar'
    'org/assertj/assertj-core/3.27.7/assertj-core-3.27.7.jar'
    'org/objenesis/objenesis/3.3/objenesis-3.3.jar'
    'net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar'
    'org/springframework/spring-test/6.2.19/spring-test-6.2.19.jar'
    'org/springframework/security/spring-security-test/6.5.11/spring-security-test-6.5.11.jar'
    'io/micrometer/micrometer-observation/1.15.12/micrometer-observation-1.15.12.jar'
    'io/micrometer/micrometer-commons/1.15.12/micrometer-commons-1.15.12.jar'
    'org/hibernate/validator/hibernate-validator/8.0.3.Final/hibernate-validator-8.0.3.Final.jar'
    'org/hamcrest/hamcrest/3.0/hamcrest-3.0.jar'
    'com/jayway/jsonpath/json-path/2.9.0/json-path-2.9.0.jar'
    'net/minidev/json-smart/2.5.2/json-smart-2.5.2.jar'
    'net/minidev/accessors-smart/2.5.2/accessors-smart-2.5.2.jar'
    'org/ow2/asm/asm/9.7.1/asm-9.7.1.jar'
)
foreach ($jar in $jars) {
    $sourceJar = Join-Path $MavenRepository $jar
    if (-not (Test-Path -LiteralPath $sourceJar)) { throw "Missing cached dependency: $sourceJar" }
    Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $libraryRoot (Split-Path $sourceJar -Leaf)) -Force
}
Get-ChildItem (Join-Path $MavenRepository 'org/springframework') -Recurse -Filter '*.jar' |
    Where-Object { $_.FullName -match '\\6\.2\.19\\|\\6\.5\.11\\|\\3\.5\.13\\' } |
    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $libraryRoot $_.Name) -Force }

$sources = @((Get-ChildItem (Join-Path $qaRoot 'src/main/java') -Recurse -Filter '*.java').FullName)
$sources += (Get-ChildItem (Join-Path $qaRoot 'src/test/java') -Recurse -Filter '*.java').FullName
$sources += (Get-ChildItem (Join-Path $backendRoot 'Huy/src/main/java/com/funews/backend/entity') -Filter '*.java').FullName
$sources += Join-Path $backendRoot 'Huy/src/main/java/com/funews/backend/repository/NewsArticleRepository.java'
$sources += Join-Path $backendRoot 'Long/src/main/java/com/funews/backend/entity/Category.java'
$sources += (Get-ChildItem (Join-Path $backendRoot 'Long/src/main/java/com/funews/backend/exception') -Filter '*.java').FullName
$sources += Join-Path $PSScriptRoot 'QaTestRunner.java'
$compileArguments = @('--release', '17', '-proc:none', '-encoding', 'UTF-8', '-parameters', '-cp', "$libraryRoot/*", '-d', $classRoot) + $sources
$ErrorActionPreference = 'Continue'
& javac @compileArguments 2>&1 | Tee-Object -FilePath (Join-Path $outputRoot 'compile.log')
$ErrorActionPreference = 'Stop'
if ($LASTEXITCODE -ne 0) { throw "QA compile failed: $LASTEXITCODE" }
$ErrorActionPreference = 'Continue'
& java "-javaagent:$libraryRoot/byte-buddy-agent-1.17.8.jar" '-cp' "$classRoot;$libraryRoot/*" 'QaTestRunner' 2>&1 |
    Tee-Object -FilePath (Join-Path $outputRoot 'tests.log')
$ErrorActionPreference = 'Stop'
if ($LASTEXITCODE -ne 0) { throw "QA tests failed: $LASTEXITCODE" }
