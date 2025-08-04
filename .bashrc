# Claude Code helper function
claude-read() {
    if [ -z "$1" ]; then
        echo "Usage: claude-read <filename>"
        return 1
    fi
    
    # Convert to Windows path if needed
    local winpath=$(cygpath -w "$1" 2>/dev/null || echo "$1")
    echo "Reading file: $winpath"
    echo "Use this in Claude Code: /read $winpath"
}

# Alias for current directory files
alias cr='claude-read'