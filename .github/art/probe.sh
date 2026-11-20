#!/usr/bin/env bash
# Cherche des photos NASA (domaine public) et produit une planche-contact pour choisir.
set -u
mkdir -p out cand
n=0
: > out/candidates.txt
for q in "carina nebula" "pillars of creation" "blue marble earth" "milky way" "aurora from space" "saturn rings" "earth night city lights" "hubble deep field" "orion nebula" "horsehead nebula" "jupiter" "lagoon nebula"; do
  json=$(curl -s -G "https://images-api.nasa.gov/search" --data-urlencode "q=$q" --data-urlencode "media_type=image" --data-urlencode "page_size=4")
  for k in 0 1 2; do
    id=$(echo "$json" | jq -r ".collection.items[$k].data[0].nasa_id // empty")
    title=$(echo "$json" | jq -r ".collection.items[$k].data[0].title // empty")
    [ -z "$id" ] && continue
    n=$((n+1))
    f=$(printf "cand/%02d_%s.jpg" "$n" "$id")
    if curl -sfL -m 60 -o "$f" "https://images-assets.nasa.gov/image/$id/$id~medium.jpg"; then
      printf "%02d\t%s\t%s\t%s\n" "$n" "$id" "$q" "$title" >> out/candidates.txt
    else
      rm -f "$f"
    fi
  done
done
ls cand | wc -l
montage -label '%t' cand/*.jpg -tile 6x -geometry 320x180+4+4 -pointsize 13 -fill white -background '#111' out/sheet.jpg
