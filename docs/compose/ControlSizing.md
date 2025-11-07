## 控件尺寸速查

本文总结 Compose 中常见控件（单个控件和容器控件）的尺寸控制方法，帮助你在实现 UI 时快速调整占用空间。

---

### 1. 单个控件尺寸

- `Modifier.size(width = …, height = …)`：同时指定宽和高。
- `Modifier.width(...) / Modifier.height(...)`：只限制某个方向。
- `Modifier.fillMaxWidth() / fillMaxHeight()`：占满父布局对应方向。
- `Modifier.fillMaxSize()`：同时占满父布局宽高。
- `Modifier.requiredWidth(...) / requiredHeight(...)`：强制尺寸，不受父布局约束。

示例：
```kotlin
Text(
    text = "Hello",
    modifier = Modifier
        .size(width = 120.dp, height = 48.dp)
        .background(Color.Gray),
    textAlign = TextAlign.Center
)
```

### 2. 容器控件尺寸

`Box` / `Row` / `Column` 等容器遵循同样的 Modifier 体系，可以用相同方式控制尺寸。

示例：
```kotlin
Box(
    modifier = Modifier
        .width(200.dp)
        .height(100.dp)
        .background(Color.LightGray)
) {
    Text("容器里的内容")
}
```

对于 `Row` / `Column`，常用 `Modifier.weight(…)` 在容器内部均匀分配控件所占空间：

```kotlin
Row(Modifier.fillMaxWidth()) {
    Text("左侧", Modifier.weight(1f))
    Text("右侧", Modifier.weight(1f))
}
```

### 3. 常见组合技巧

- `padding()`：通过内边距微调控件与内容的距离，可与上述尺寸 Modifier 组合使用。
- `wrapContentWidth()` / `wrapContentHeight()`：约束为内容大小，可搭配 `align()`、`offset()` 等调整位置。
- 自定义尺寸 Modifier：可以通过 `Modifier.layout { measurable, constraints -> … }` 实现复杂的测量逻辑。

掌握这些常用方式，就能在 Compose 中快速控制单个控件和容器的尺寸。
