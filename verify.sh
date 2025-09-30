#!/bin/bash

echo "🔍 NextGenCaller Verification Script"
echo "======================================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check file
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1"
        return 1
    fi
}

# Function to check directory
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} Directory: $1"
        return 0
    else
        echo -e "${RED}✗${NC} Directory: $1"
        return 1
    fi
}

BASE_PATH="app/src/main/java/com/nextgencaller"
MISSING_COUNT=0
PRESENT_COUNT=0

echo ""
echo "📁 Checking Directory Structure..."
echo "-----------------------------------"

directories=(
    "$BASE_PATH/data/local/entity"
    "$BASE_PATH/data/local/dao"
    "$BASE_PATH/data/remote"
    "$BASE_PATH/data/repository"
    "$BASE_PATH/domain/model"
    "$BASE_PATH/domain/usecase"
    "$BASE_PATH/di"
    "$BASE_PATH/services"
    "$BASE_PATH/utils"
    "$BASE_PATH/worker"
    "$BASE_PATH/presentation/call"
    "$BASE_PATH/presentation/home"
    "$BASE_PATH/presentation/contacts"
    "$BASE_PATH/presentation/history"
    "$BASE_PATH/presentation/theme"
)

for dir in "${directories[@]}"; do
    check_dir "$dir"
done

echo ""
echo "📄 Checking Critical Files..."
echo "-----------------------------------"

# Critical files array
files=(
    "$BASE_PATH/MainApplication.kt"
    "$BASE_PATH/data/local/AppDatabase.kt"
    "$BASE_PATH/data/remote/WebRTCClient.kt"
    "$BASE_PATH/data/remote/SignalingClient.kt"
    "$BASE_PATH/data/repository/CallRepositoryImpl.kt"
    "$BASE_PATH/domain/usecase/ManageCallUseCase.kt"
    "$BASE_PATH/presentation/MainActivity.kt"
    "$BASE_PATH/presentation/call/CallViewModel.kt"
    "$BASE_PATH/services/CallForegroundService.kt"
    "app/build.gradle.kts"
    "app/src/main/AndroidManifest.xml"
)

for file in "${files[@]}"; do
    if check_file "$file"; then
        ((PRESENT_COUNT++))
    else
        ((MISSING_COUNT++))
    fi
done

echo ""
echo "📊 Statistics"
echo "-----------------------------------"
echo -e "Present: ${GREEN}$PRESENT_COUNT${NC}"
echo -e "Missing: ${RED}$MISSING_COUNT${NC}"

# Count total Kotlin files
KOTLIN_COUNT=$(find $BASE_PATH -name "*.kt" 2>/dev/null | wc -l)
echo -e "Total Kotlin files: ${YELLOW}$KOTLIN_COUNT${NC}"

# Check build
echo ""
echo "🔨 Checking Build..."
echo "-----------------------------------"
if ./gradlew tasks > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Gradle build system working"
else
    echo -e "${RED}✗${NC} Gradle build system has issues"
fi

echo ""
if [ $MISSING_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ All critical files present!${NC}"
    echo "Next step: Run ./gradlew build"
else
    echo -e "${YELLOW}⚠️  Some files are missing${NC}"
    echo "Review the list above and add missing files"
fi