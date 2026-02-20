# AE2UEL 智能样板系统 (Smart Pattern System)
*AE2 Unofficial Extended Life 智能样板系统 - 基于矿物辞典的通配符合成样板*

> 🧪 **v1.0.5 发布！** 详见 [发布指南](RELEASE_GUIDE.md) 和 [更新日志](CHANGELOG.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-14.23.5.2847-red.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-8+-blue.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0.5-orange.svg)](CHANGELOG.md)

一个为 AE2UEL (Applied Energistics 2 Unofficial Extended Life) 设计的智能样板系统，通过矿物辞典实现通配符匹配，让单个样板可以自动匹配整个矿物辞典系列（如所有金属锭→板的转换）。

## v1.0.5 更新摘要
- 新增 JEI 幽灵物品拖拽到样板槽位，并通过网络包同步到服务端。
- 新增 MMCE 兼容层：`ItemStack.EMPTY`、`JsonUtils` 签名兼容，以及样板供应器 GUI lambda 兼容钩子。
- 修复 MMCE 预加载 `NoSuchMethodError` 与样板供应器界面校验错误导致的打开失败。
- 修复 MMCE 中样板集成会影响同控制器其它样板生效的问题。
- GUI 贴图拆分：智能样板编辑器独立使用 `pattern_editor_smart.png`。
- GUI 背景绘制加入 `+2px` X 偏移，保持你修复贴图前后的槽位视觉对齐。
- 配置扩展：支持通过 `customOrePrefixes` 增加新的矿辞前缀类型（如 `ore`）。

## ✨ 核心特性

### 🎯 智能样板扩展
- **矿物辞典通配符**: 利用 Forge 矿物辞典系统，支持 `ingot*`、`plate*` 等通配符模式
- **自动批量生成**: 一个样板可自动扩展为多个具体配方（如 `ingot*→plate*` 根据矿物辞典自动识别所有已注册的金属配方）
- **AE2UEL 完全兼容**: 实现 `ICraftingPatternItem` 接口，无缝集成 AE2 ME 接口
- **样板扩展器方块**: 专用方块用于编辑和管理智能样板

## 📦 安装要求

### 必需依赖
- **Minecraft**: 1.12.2
- **Minecraft Forge**: 14.23.5.2847+
- **AE2UEL**: 兼容版本

## 🚀 快速开始

### 安装步骤
1. 安装 Minecraft 1.12.2 和 Forge 14.23.5.2847+
2. 下载并安装 AE2UEL 兼容版本
3. 将本模组 jar 文件放入 `mods` 文件夹
4. 启动游戏

### 基本使用流程

#### 1. 获取物品
```
/give @s sampleintegration:pattern_integrations    # 智能样板物品
/give @s sampleintegration:pattern_expander        # 样板扩展器方块
```

#### 2. 编辑样板
1. **放置样板扩展器方块**在AE中
2. **手持智能样板物品**，右键点击方块打开编辑界面
3. **拖拽输入物品**到左侧输入槽（例如：铁锭）
4. **拖拽输出物品**到右侧输出槽（例如：铁板）
5. **调整数量**: 中键即可设置数量

#### 3. 使用样板
1. 将编码好的智能样板插入 **AE2 ME 接口 等等**
2. ME 系统会根据矿物辞典自动识别并扩展样板为多个具体配方（数量取决于已安装的模组）

#### 4. 清除样板
- **Shift + 右键**点击手中的样板物品即可清除 NBT 数据

## 📖 详细功能说明

### 支持的通配符类型
当前版本支持以下矿物辞典前缀的通配符（8种）：
- `ingot*` → 所有金属锭
- `plate*` → 所有金属板
- `nugget*` → 所有金属粒
- `block*` → 所有金属块
- `gear*` → 所有齿轮
- `rod*` → 所有杆/棒
- `wire*` → 所有线缆
- `dust*` → 所有粉末
- 也可通过config中的配置文件添加


### 样板扩展示例
**输入**: `ingotCopper` (铜锭) → **输出**: `plateCopper` (铜板)

系统根据矿物辞典自动扩展配方（通常19+种）：
```
铁锭 → 铁板
铜锭 → 铜板
金锭 → 金板
锡锭 → 锡板
银锭 → 银板
铅锭 → 铅板
铝锭 → 铝板
镍锭 → 镍板
铂锭 → 铂板
钨锭 → 钨板
... (实际数量取决于已安装的模组)
```

> **提示**: 只要矿物辞典中存在 `ingotX` ↔ `plateX` 等对应条目，系统会自动识别并扩展，**不限制具体材料种类**。支持所有使用矿物辞典的模组（热力膨胀、通用机械、沉浸工程、格雷科技等）。实际支持的种类由游戏中已安装的模组决定，理论上可支持数十甚至上百种材料。

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 🙏 致谢

- **AE2UEL 团队**: 提供优秀的 AE2 扩展版本
- **Minecraft Forge 社区**: 强大的模组开发框架
- **CleanroomMC**: RetroFuturaGradle 构建系统
- **矿物辞典系统**: Forge 统一物品标签系统

## 📞 联系方式

- **问题反馈**: 在 GitHub 提交 Issue
- **功能建议**: 在 GitHub 提交 Feature Request

**版本**: v1.0.5 | **Minecraft**: 1.12.2 | **Forge**: 14.23.5.2847+ | **AE2UEL**: 兼容版本

*基于 CleanroomMC 的 RetroFuturaGradle 构建* | *完全开源，MIT 许可*

