#include "../../platform-android/src/main/cpp/repetition_guard.h"
#include <cassert>
#include <iostream>

int main() {
    assert(repeated_numbered_body(
        "1. Verify the material certificate before release.\n"
        "2. Verify the material certificate before release.\n"
        "3. Verify the material certificate before release."));
    assert(repeated_numbered_body(
        "**10.** 检查供应商提供的材料证明和批次记录。\n"
        "**11.** 检查供应商提供的材料证明和批次记录。\n"
        "**12.** 检查供应商提供的材料证明和批次记录。"));
    assert(!repeated_numbered_body(
        "1. Verify the material certificate before release.\n"
        "2. Measure three samples and record the values.\n"
        "3. Escalate any mismatch to quality engineering."));
    assert(!repeated_numbered_body("1. PASS\n2. PASS\n3. PASS"));
    assert(!repeated_numbered_body(
        "1. Verify the material certificate before release.\n"
        "A paragraph interrupts the numbered sequence.\n"
        "2. Verify the material certificate before release.\n"
        "3. Verify the material certificate before release."));
    std::cout << "PASS numbered-body repetition guard\n";
}
