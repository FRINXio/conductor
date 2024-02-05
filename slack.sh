#!/bin/bash
VERSION="${1}"
API_SLACK_WEBHOOK_URL="${2}"
json_content=$(cat diff.txt)
json_string="\\n \`\`\`$json_content\`\`\`"


json_string='{
	"blocks": [
		{
			"type": "header",
			"text": {
				"type": "plain_text",
				"text": "Conductor API changes: '"${VERSION}"'",
				"emoji": true
			}
		},
		{
			"type": "divider"
		},
		{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "'"$json_string"'"
			}
		}
	]
}'

curl -v -X POST -H 'Content-type: application/json' --data "$json_string" $API_SLACK_WEBHOOK_URL
 