while IFS= read -r line || [[ -n "$line" ]]; do
    echo "Text read from file: $line"
    curl -X POST -H "Content-Type: application/json"  --data "$line"  http://localhost:9090/createNode
done < "$1"

# curl -X POST -H "Content-Type: application/json" --data-binary @- http://localhost:9090/createNode <<EOF
