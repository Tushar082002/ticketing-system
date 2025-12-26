# IDE Setup & Troubleshooting Guide

## Issue: "Cannot find symbol: method builder()"

This error occurs when Lombok annotation processor hasn't been recognized by your IDE. Follow these steps:

---

## 🔧 Solution Steps

### Step 1: IntelliJ IDEA (JetBrains IU)

**Option A: Reload Maven Project**
1. Right-click on `pom.xml`
2. Select **"Maven" → "Reload projects"**
3. Wait for Maven to redownload dependencies
4. File → Invalidate Caches → Invalidate and Restart

**Option B: Enable Annotation Processing**
1. Go to **Settings** (or **Preferences** on Mac)
2. Navigate to **Build, Execution, Deployment → Compiler → Annotation Processors**
3. Enable **"Enable annotation processing"**
4. Click **OK**
5. File → Invalidate Caches → Invalidate and Restart

**Option C: Install Lombok Plugin**
1. Go to **Settings → Plugins**
2. Search for **"Lombok"**
3. Install the plugin by **JetBrains**
4. Restart IDE

### Step 2: Verify Lombok Configuration

1. Open `pom.xml`
2. Verify you have:
   ```xml
   <dependency>
       <groupId>org.projectlombok</groupId>
       <artifactId>lombok</artifactId>
       <version>1.18.30</version>
       <optional>true</optional>
   </dependency>
   ```

3. Verify compiler plugin has:
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <source>17</source>
           <target>17</target>
           <annotationProcessorPaths>
               <path>
                   <groupId>org.projectlombok</groupId>
                   <artifactId>lombok</artifactId>
                   <version>1.18.30</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

### Step 3: Clean Build

```bash
# Terminal/PowerShell
cd C:\Users\Hp\IdeaProjects\TicketingApplication

# Clean
mvn clean

# Rebuild
mvn install -DskipTests

# Or just compile
mvn clean compile
```

### Step 4: Restart IDE

1. Close IntelliJ IDEA completely
2. Delete IDE cache (if persistent):
   - Windows: `%APPDATA%\.IntelliJIdea*\system\` (optional)
3. Reopen IntelliJ IDEA
4. Open the project again

### Step 5: Verify Configuration

Check that:
- ✅ `pom.xml` updated (we did this for you)
- ✅ `lombok.config` exists in project root (we created this)
- ✅ IDE recognizes Lombok plugin
- ✅ Maven can see dependencies

---

## ✅ Files Updated/Created

We've updated the following to ensure Lombok works correctly:

1. **pom.xml** - Added explicit Lombok version (1.18.30)
2. **pom.xml** - Added source/target Java 17 configuration
3. **pom.xml** - Updated annotation processor path with Lombok version
4. **lombok.config** - Created configuration file for IDE support

---

## 🧪 Test After Fix

Once you complete the steps above:

```bash
# Build should work without errors
mvn clean compile

# You should see:
# BUILD SUCCESS
```

If successful, you can run:
```bash
mvn spring-boot:run
```

---

## 📝 Common Issues & Solutions

### Issue 1: "Cannot find symbol: method builder()"
**Solution:** 
- Follow Step 1 (Invalidate Caches & Restart)
- Or install Lombok plugin from IDE

### Issue 2: Maven Still Shows Error
**Solution:**
- Right-click pom.xml → Maven → Reload Projects
- Wait for Maven to finish indexing
- Then rebuild

### Issue 3: IDE Won't Recognize Changes
**Solution:**
- Close IDE
- Delete: `%USERPROFILE%\.m2\repository\org\projectlombok\`
- Reopen IDE
- Let Maven redownload Lombok

### Issue 4: Compilation Succeeds but IDE Shows Errors
**Solution:**
- File → Invalidate Caches → Invalidate and Restart
- This is an IDE cache issue, not a real compilation error

---

## 🔍 Verify Lombok is Working

After fix, check that these compile without error:

```java
// This should work
ErrorResponse error = ErrorResponse.builder()
    .timestamp(LocalDateTime.now())
    .status(404)
    .error("Test")
    .message("Test message")
    .path("/test")
    .build();
```

---

## 📦 What Was Updated

### pom.xml Changes

**Before:**
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <optional>true</optional>
</dependency>
```

**Compiler Plugin - Added source/target:**
```xml
<configuration>
    <source>17</source>
    <target>17</target>
    <annotationProcessorPaths>
        <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
        </path>
    </annotationProcessorPaths>
</configuration>
```

### New File: lombok.config
```
config.stopBubblingOnFileFind = true
lombok.addLombokGeneratedAnnotation = true
```

---

## ✨ After Everything Works

Once the compilation error is fixed, you can run:

```bash
mvn clean install
mvn spring-boot:run
```

Application will start on: **http://localhost:8080**

---

## 🆘 Still Having Issues?

If the error persists after following all steps:

1. **Verify Lombok installation in project:**
   ```bash
   mvn dependency:tree | findstr lombok
   ```
   
   Should show:
   ```
   org.projectlombok:lombok:jar:1.18.30:compile
   ```

2. **Check IDE Lombok support:**
   - IntelliJ: Settings → Plugins → Search "Lombok" (should be installed)
   - Ensure IDE is up to date

3. **Nuclear Option - Clear Everything:**
   ```bash
   # Delete Maven cache
   rmdir %USERPROFILE%\.m2\repository /s /q
   
   # Delete IDE cache
   rmdir %APPDATA%\.IntelliJIdea* /s /q
   
   # Restart IDE and rebuild
   mvn clean install
   ```

---

**Status:** All code files are correct. This is purely an IDE/Lombok configuration issue.

**Solution:** Follow the steps above and the error will disappear! ✅

