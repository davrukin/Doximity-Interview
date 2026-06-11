#!/bin/bash

echo "Running pre-commit checks..."
echo "Tasks: ktlintCheck, testDebugUnitTest, detekt, assembleDebug, assembleRelease"

./gradlew ktlintCheck testDebugUnitTest detekt assembleDebug assembleRelease

STATUS=$?

if [ $STATUS -ne 0 ]; then
    echo "=================================================================="
    echo "❌ Pre-commit checks failed. Please fix the errors before committing."
    echo "=================================================================="
    exit 1
fi

echo "=================================================================="
echo "✅ All checks passed! Committing..."
echo "=================================================================="
exit 0
