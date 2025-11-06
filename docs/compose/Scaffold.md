## Scaffold 介绍

`Scaffold` 是 Jetpack Compose 提供的一个高阶布局容器，用来快速搭建符合 Material Design 规范的基础页面结构。它把常见的 UI 区域（例如顶部栏、底部栏、悬浮按钮、抽屉等）都预留好了“插槽”，你只需要把对应的 Composable 塞进去即可，不必手写一堆布局代码。

换句话说，`Scaffold` = “标准页面骨架 + 你自定义的内容”。对于刚接触 Compose 的新手，`Scaffold` 可以帮助你把注意力集中在业务内容，而不是重复搭建页面框架。

---

## Scaffold 的主要槽位

`Scaffold` 中常用的几个槽位（slot）如下：

- `topBar`：顶部应用栏（`TopAppBar`、`CenterAlignedTopAppBar` 等）
- `bottomBar`：底部导航栏或工具栏（例如 `NavigationBar`、`BottomAppBar`）
- `floatingActionButton`：悬浮操作按钮（`FloatingActionButton`）
- `drawerContent`：侧边抽屉菜单
- `content`（lambda 参数）：页面主体内容，默认占据剩余空间

当然，你可以只实现自己需要的槽位，没有用到的槽位可以直接忽略。

---

## Scaffold 基本用法示例

下面给出一个简单的示例，展示如何使用 `Scaffold` 组合顶部栏、底部栏和主体正文内容：

```kotlin
@Composable
fun ScaffoldSample() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("主页", "消息", "设置")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scaffold 示例") }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(Icons.Filled.Home, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Add, contentDescription = "添加")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("当前选中：${tabs[selectedTab]}")
        }
    }
}
```

### 代码解析

- `Scaffold` 内部会根据 `topBar`、`bottomBar` 等槽位自动安排布局，避免内容被覆盖。
- `innerPadding` 参数包含了 `Scaffold` 为了容纳顶部 / 底部栏自动计算出的 `PaddingValues`。将它传给主体内容可以保证正文不会被导航栏遮挡。
- 主体内容里可以自由使用 `Column`、`LazyColumn` 等布局组件。

---

## 为什么推荐使用 Scaffold

1. **统一的页面骨架**：搭建多 tab、带顶部栏的页面结构时无需重复编写相同布局代码。
2. **与 Material 组件无缝协作**：例如 `TopAppBar`、`NavigationBar`、`FloatingActionButton` 都能直接放进对应槽位。
3. **自动处理内边距**：`innerPadding` 可以防止主体内容被顶部/底部栏覆盖，省去手工计算。
4. **易于扩展**：可以按需加入抽屉、浮动按钮、Snackbar 等高级交互区域。
5. **更符合官方最佳实践**：增强代码可读性，也方便团队协作和 UI 规范统一。

---

## 小结

对于初学 Compose 的开发者：

- 如果你的页面需要顶部栏、底部导航、悬浮按钮等结构，优先考虑 `Scaffold`。
- 如果仅仅是非常简单的内容，也可以直接使用 `Column` / `Box` 等基础布局；但随着页面复杂度提升，`Scaffold` 会带来更高的可维护性。

掌握 `Scaffold` 就等于掌握了 Compose 中“页面壳层”的搭建方式，后续你只需关注每个槽位里要放什么内容即可。

