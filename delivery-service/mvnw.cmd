@echo off
setlocal EnableExtensions

set "MAVEN_OPTS=-Dmaven.repo.local=E:\QuickBite-App-Local\QuickBite-Backend-L\.m2\repository"
set "MVN_CMD=C:\Users\HP\.m2\wrapper\dists\apache-maven-3.9.14\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd"
if not exist "%MVN_CMD%" set "MVN_CMD=C:\Users\HP\.m2\wrapper\dists\apache-maven-3.9.14-bin\1cb7fhup6b5n3bed6kckbrnspv\apache-maven-3.9.14\bin\mvn.cmd"
if not exist "%MVN_CMD%" (
  echo Cannot find Maven installation or wrapper cache.>&2
  exit /b 1
)

call "%MVN_CMD%" %*
exit /b %errorlevel%
