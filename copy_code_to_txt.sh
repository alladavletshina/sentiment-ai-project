#!/bin/bash

# Называем целевой файл, куда будем собирать код
output_file="all_code.txt"

# Чистка предыдущего содержания файла, если он существует
if [[ -f "$output_file" ]]; then
    rm "$output_file"
fi

# Проходим по всем файлам проекта рекурсивно и добавляем их содержимое в all_code.txt
for file in $(find . -name '*.java' -o -name '*.kt' -o -name '*.py' -o -name '*.go' -o -name '*.rb' -o -name '*.yaml' -o -name 'application.properties'  -o -name '*.xml'); do
    # Игнорируем двоичные файлы и скрытые папки типа ".git"
    if ! grep -qE '^\.(git|idea)' <<< "$file"; then
        echo "" >> "$output_file" # пустая строка между файлами
        echo "# File: $file" >> "$output_file" # отображаем путь к файлу
        cat "$file" >> "$output_file" # добавляем содержимое файла
    fi
done

echo "Весь код собран в файл $output_file"