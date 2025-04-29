
#!/bin/bash
 
# echo 3.3.0.0

# Determine the directory of the script
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
target_dir="$script_dir/../j-lawyer-server-entities/src/java/db/migration"

# Extract version numbers, convert to dotted format, sort, and get the highest
highest_version=$(
  find "$target_dir" -maxdepth 1 -type f -name 'V*_*.sql' |
  sed -n 's|.*/V\([0-9_]\+\)__.*\.sql$|\1|p' |
  tr '_' '.' |
  sort -V |
  tail -n 1
)

echo $highest_version
 
