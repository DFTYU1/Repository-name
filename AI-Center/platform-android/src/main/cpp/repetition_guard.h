#pragma once
#include <cctype>
#include <string>
#include <vector>

// Detect a common small-model degeneration where only the visible list number
// changes while the substantive body repeats. This is intentionally narrow:
// three consecutive, sufficiently long, completed numbered items must have the
// same normalized body. Normal lists with distinct bodies are not stopped.
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
        const size_t end = answer.find('\n', start);
        const std::string body = normalized_numbered_body(answer.substr(
            start, end == std::string::npos ? std::string::npos : end - start));
        if (!body.empty()) bodies.push_back(body);
        else bodies.clear();
        if (bodies.size() >= 3) {
            const size_t n = bodies.size();
            if (bodies[n - 1] == bodies[n - 2] && bodies[n - 2] == bodies[n - 3]) return true;
        }
        if (end == std::string::npos) break;
        start = end + 1;
    }
    return false;
}
