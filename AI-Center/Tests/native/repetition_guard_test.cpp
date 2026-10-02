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
    assert(repeated_numbered_body(
        "10. 变更合同附件；11. 变更合同签署日期；12. 变更合同签署地点；"
        "13. 变更合同签署人；14. 变更合同签署日期；15. 变更合同签署地点；"
        "16. 变更合同签署人；17. 变更合同签署日期；18. 变更合同签署地点；"
        "19. 变更合同签署人；"));
    assert(!repeated_numbered_body(
        "1. Verify the material certificate before release.\n"
        "2. Measure three samples and record the values.\n"
        "3. Escalate any mismatch to quality engineering."));
    assert(!repeated_numbered_body("1. PASS\n2. PASS\n3. PASS"));
    assert(!repeated_numbered_body(
        "1. 记录变更前供应商和物料状态；2. 记录变更后的供应商和物料状态；"
        "3. 说明本次供应商变更的完整原因；4. 保存验证方法和验证结论；"
        "5. 定义新物料正式切入批次；6. 定义仓库旧料处置规则；"
        "7. 完成质量工程采购部门会签；8. 保存批次和订单追溯信息；"));
    assert(!repeated_numbered_body(
        "1. 变更合同签署日期；2. 变更合同签署地点；3. 变更合同签署人；"
        "4. 变更合同签署日期；5. 变更合同签署地点；6. 变更合同签署人；"));
    assert(!repeated_numbered_body(
        "1. Verify the material certificate before release.\n"
        "A paragraph interrupts the numbered sequence.\n"
        "2. Verify the material certificate before release.\n"
        "3. Verify the material certificate before release."));
    std::cout << "PASS numbered-body repetition guard\n";
}
