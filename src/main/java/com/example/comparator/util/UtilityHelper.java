git log --since="2 years ago" --date=short --pretty=format:"__COMMIT__%h|%an|%ad|%s" --name-only |
awk -F"|" '
/^__COMMIT__/ {
if (commit_hash) {
print commit_hash "," author "," date ",\"" message "\",\"" files "\""
}
commit_hash=$2
author=$3
date=$4
message=$5
files=""​
next
}
NF {
files = (files ? files "; " : "") $0
}
END {
if (commit_hash) {
print commit_hash "," author "," date ",\"" message "\",\"" files "\""
}
}' > commit_report_with_files.csv



