#!/usr/bin/env bash

echo "Stopping Atlas services..."

if command -v pkill >/dev/null 2>&1; then

    pkill -f "spring-boot:run" || true
    pkill -f "java" || true

else

    taskkill //F //IM java.exe >/dev/null 2>&1 || true

fi

echo ""
echo "Atlas services stopped."