#pragma once
#include <string>
// Escape only untrusted content, before inserting it into the fixed ChatML template.
// Token counting and generation share this exact formatter. Not a general prompt-injection proof.
inline std::string escape_chat_special_tokens(const std::string &input) {
    std::string result; result.reserve(input.size());
    for (size_t i=0;i<input.size();++i) {
        if (input[i]=='<' && i+1<input.size() && input[i+1]=='|') result+="＜";
        else result+=input[i];
    }
    return result;
}
