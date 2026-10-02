#pragma once
#include <cctype>
#include <string>
#include <vector>

// Detect common small-model degeneration where only the visible list number
// changes while substantive bodies repeat. Keep this deliberately narrow:
// completed numbered items must form three copies of the same 1-3 item cycle,
// and every body must be sufficiently long. Both newline lists and compact
// semicolon-separated lists are supported. Normal lists remain unaffected.
inline std::string normalized_numbered_body(std::string line) {
    while (!line.empty() && std::isspace(static_cast<unsigned char>(line.front()))) line.erase(0, 1);
    if (line.size() >= 2 && line[0] == '*' && line[1] == '*') line.erase(0, 2);
    size_t position = 0;
    while (position < line.size() && std::isdigit(static_cast<unsigned char>(line[position]))) position++;
    if (position == 0) return {};
    if (position + 1 < line.size() && line[position] == '*' && line[position + 1] == '*') position += 2;
    if (position >= line.size()) return {};
    const unsigned char marker = static_cast<unsigned char>(line[position]);
    if (marker == '.' || marker == ')' || marker == ':' || marker == '-') position++;
    else if (position + 2 < line.size() && marker == 0xE3 &&
             static_cast<unsigned char>(line[position + 1]) == 0x80 &&
             static_cast<unsigned char>(line[position + 2]) == 0x81) position += 3; // U+3001
    else return {};
    std::string body;
    bool spacing = false;
    for (; position < line.size(); position++) {
        const unsigned char value = static_cast<unsigned char>(line[position]);
        if (std::isspace(value)) { spacing = !body.empty(); continue; }
        if (spacing) body.push_back(' ');
        spacing = false;
        body.push_back(static_cast<char>(std::tolower(value)));
    }
    while (!body.empty() && std::isspace(static_cast<unsigned char>(body.back()))) body.pop_back();
    return body.size() >= 20 ? body : std::string{};
}

inline bool repeated_numbered_body(const std::string &answer) {
    std::vector<std::string> bodies;
    size_t start = 0;
    while (start < answer.size()) {
        size_t end = start;
        size_t separator_bytes = 0;
        while (end < answer.size()) {
            const unsigned char value = static_cast<unsigned char>(answer[end]);
            if (value == '\n' || value == ';') {
                separator_bytes = 1;
                break;
            }
            if (end + 2 < answer.size() && value == 0xEF &&
                static_cast<unsigned char>(answer[end + 1]) == 0xBC &&
                static_cast<unsigned char>(answer[end + 2]) == 0x9B) {
                separator_bytes = 3; // U+FF1B full-width semicolon
                break;
            }
            end++;
        }
        const std::string body = normalized_numbered_body(answer.substr(start, end - start));
        if (!body.empty()) bodies.push_back(body);
        else bodies.clear();
        const size_t n = bodies.size();
        for (size_t period = 1; period <= 3 && n >= period * 3; period++) {
            bool repeated = true;
            for (size_t offset = 0; offset < period * 2; offset++) {
                if (bodies[n - 1 - offset] != bodies[n - 1 - offset - period]) {
                    repeated = false;
                    break;
                }
            }
            if (repeated) return true;
        }
        if (separator_bytes == 0) break;
        start = end + separator_bytes;
    }
    return false;
}
