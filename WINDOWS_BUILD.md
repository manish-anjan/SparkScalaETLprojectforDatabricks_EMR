# Windows Build Instructions

## For Windows PowerShell Users

### Prerequisites
1. **Java 11 or higher installed:**
   ```powershell
   java -version
   ```
   Should show: `openjdk version "11" or higher`

2. **SBT installed:**
   ```powershell
   sbt sbtVersion
   ```
   Should show: `1.12.4 or higher`

   If not installed, download from: https://www.scala-sbt.org/download.html

### Step 1: Navigate to Project
```powershell
cd "E:\POC code\SparkScalaETLprojectforDatabricks_EMR"
```

### Step 2: Clear Old Caches (Important!)
```powershell
# Remove SBT cache (careful, these are hidden)
Remove-Item -Path "$env:USERPROFILE\.sbt\" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "$env:USERPROFILE\.ivy2\" -Recurse -Force -ErrorAction SilentlyContinue

# Remove project caches
Remove-Item -Path "project\target\" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "target\" -Recurse -Force -ErrorAction SilentlyContinue
```

### Step 3: Update Dependencies
```powershell
sbt update
```
Wait for this to complete (may take 2-5 minutes on first run)

### Step 4: Compile Project
```powershell
sbt clean compile
```

**Expected output:**
```
[info] Compiling 6 Scala sources...
[success] Total time: XXs, completed ...
```

### Step 5: Build Assembly JAR (Optional)
```powershell
sbt assembly
```

**Expected output:**
```
[info] Including: ... (many files)
[info] Assembling into target\SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar
[success] Total time: XXs, completed ...
```

## Troubleshooting on Windows

### Issue: "sbt: The term 'sbt' is not recognized"

**Solution:**
1. Install SBT from: https://www.scala-sbt.org/download.html
2. Add SBT to PATH:
   - Right-click "This PC" → Properties
   - Click "Advanced system settings"
   - Click "Environment Variables"
   - Under "User variables", click "New"
   - Variable name: `Path`
   - Variable value: `C:\Program Files\sbt\bin` (or where you installed SBT)
   - Restart PowerShell

### Issue: "value apache is not a member of org"

**Solution:**
```powershell
# This should now be fixed, but if you still see it:

# 1. Clear everything
Remove-Item -Path "$env:USERPROFILE\.sbt\" -Recurse -Force
Remove-Item -Path "$env:USERPROFILE\.ivy2\" -Recurse -Force
Remove-Item -Path "project\target\" -Recurse -Force
Remove-Item -Path "target\" -Recurse -Force

# 2. Check Java version
java -version
# Must be 11 or higher

# 3. Update again
sbt update

# 4. Recompile
sbt clean compile
```

### Issue: "Dependency download fails"

**Solution A: Check Internet**
```powershell
# Test connectivity to Maven Central
Test-Connection repo1.maven.org -Count 1
```

**Solution B: Use Corporate Proxy**

If behind proxy, create/update `%USERPROFILE%\.sbt\repositories`:
```
[repositories]
  local
  my-ivy-proxy-1: http://proxy.example.com:8080/ivy2/, [organization]/[module]/([revision]/)?[type]s.[ext]
  my-maven-proxy-1: http://proxy.example.com:8080/maven2/
  java.net-maven2: https://repository.jboss.org/nexus/content/repositories/releases/
```

### Issue: "Out of Memory"

**Solution:**
```powershell
# Set environment variable for more memory
$env:SBT_OPTS = "-Xmx4g -XX:+UseG1GC"

# Then run sbt
sbt clean compile
```

### Issue: Compiling stuck/hanging

**Solution:**
```powershell
# Kill SBT process
Get-Process java | Stop-Process -Force

# Clear target
Remove-Item -Path "target\" -Recurse -Force

# Try again
sbt clean compile
```

## Verification Commands

```powershell
# Check Java version
java -version

# Check SBT version
sbt sbtVersion

# Check Scala version
sbt scalaVersion

# List all dependencies
sbt dependencyTree

# Run compilation with verbose output
sbt -v clean compile

# Check for specific dependency
sbt "show libraryDependencies"
```

## Complete Fresh Start (Nuclear Option)

If nothing works, do a complete reset:

```powershell
# 1. Close all SBT/Java processes
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force

# 2. Remove ALL caches
Remove-Item -Path "$env:USERPROFILE\.sbt\" -Recurse -Force
Remove-Item -Path "$env:USERPROFILE\.ivy2\" -Recurse -Force
Remove-Item -Path "$env:APPDATA\sbt\" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -Path "project\target\" -Recurse -Force
Remove-Item -Path "target\" -Recurse -Force

# 3. Go to project directory
cd "E:\POC code\SparkScalaETLprojectforDatabricks_EMR"

# 4. Start fresh
sbt update
sbt clean compile
```

## Success Checklist

After running `sbt clean compile`:

- [x] No "value apache is not a member of org" error
- [x] No download errors
- [x] No out of memory errors
- [x] Final message shows "[success]"
- [x] All 6 Scala source files compiled
- [x] No warnings about missing dependencies

## Next Steps After Successful Compile

```powershell
# Build assembly JAR
sbt assembly

# Verify JAR was created
ls target\*.jar

# You should see:
# SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar
```

## Getting Help

If you're still stuck:

1. **Post the full error message** with lines above and below it
2. **Check the build output** with: `sbt clean compile 2>&1 | Out-File build.log; cat build.log`
3. **Verify versions**: `java -version`, `sbt sbtVersion`
4. **Check available disk space**: `Get-Volume`

---

**Last Updated**: February 24, 2026
**Windows PowerShell Verified**: Yes
**Scala Version**: 2.12.18
**Spark Version**: 3.5.0

