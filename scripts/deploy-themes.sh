#!/bin/bash
# 主题文件部署脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
THEMES_DIR="$PROJECT_ROOT/themes"

echo "部署主题文件..."

# 检查主题目录
if [ ! -d "$THEMES_DIR" ]; then
    echo "错误: 找不到主题目录"
    exit 1
fi

# 统计主题数量
THEME_COUNT=$(find "$THEMES_DIR" -maxdepth 1 -type d | grep -v "^$THEMES_DIR$" | wc -l)

echo "找到 $THEME_COUNT 个主题"
echo ""
echo "主题文件位置: $THEMES_DIR"
echo ""
echo "下一步:"
echo "1. 在 nginx 配置中添加主题文件代理:"
echo "   location /themes/ {"
echo "       alias $THEMES_DIR/;"
echo "       expires 7d;"
echo "   }"
echo ""
echo "2. 在 New-API 的 location / 中添加主题注入:"
echo "   sub_filter '</head>' '<link rel=\"stylesheet\" href=\"/themes/theme-switcher.css\"><script src=\"/themes/theme-switcher.js\"></script></head>';"
echo "   sub_filter_once on;"
echo "   proxy_set_header Accept-Encoding \"\";"
echo ""
echo "详细配置说明请查看: $PROJECT_ROOT/nginx/README.md"
