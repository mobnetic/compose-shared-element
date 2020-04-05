# compose-shared-element
Proof of concept exploration of how to implement SharedElement transition in Jetpack Compose.  
Inspired by Flutter Hero widget.  

Transition consists of following animations:
- Position
- Scale
- Crossfade (which makes it work on any kind of element, e.g. `Text`)
<br>

| **UsersListScreen**:<br><img src="screenshots/UsersListScreen.png" alt="UsersListScreen" width="180"/><br><br>**UserDetailScreen**:<br><img src="screenshots/UserDetailsScreen.png" alt="UserDetailScreen" width="180"/> | Slowed down transition animation:<br><img src="screenshots/TransitionAnimation.gif" alt="TransitionAnimation" width="390"/> |
| --- | --- |


# Usage
1. Define `FROM` and `TO` elements with shared tag
```kotlin
@Composable
fun ScreenA() {
  // ...
  
    SharedElement(tag = "tag", type = SharedElementType.FROM) {
        Image(image, Modifier.preferredSize(48.dp))
    }
  
  // ...
}

@Composable
fun ScreenB() {
  // ...
  
    SharedElement(tag = "tag", type = SharedElementType.TO) {
        Image(image, Modifier.preferredSize(200.dp))
    }
    
  // ...
}
```

2. Make `SharedElementsRoot` a common parent of `ScreenA` and `ScreenB`. It doesn't have to be their direct parent.  
```kotlin
SharedElementsRoot {
    // change between ScreenA and ScreenB
}
```

3. Transition starts automatically when second `SharedElement` is detected

# Sample
See sample usage with `UsersListScreen` and `UserDetailsScreen` [here](sample/src/main/java/com/mobnetic/compose/sharedelement/sample/MainActivity.kt)

# Issues
### Blinking when going back to `UsersListScreen`
When going from `UsersListScreen` to `UserDetailsScreen` everything works fine. The sequence is as follows:
1. `startElement` from previous screen is gone
2. `endElement` calls `onElementRegistered` and hides itself (by using `alpha = 0`)
3. `transition` is set to `SharedElementTransition.WaitingForEndElementPosition` and `invalidateTransitionsOverlay()` is called.
4. `SharedElementTransitionsOverlay` reads that transition and draws `SharedElementTransitionPlaceholder` of `startElement` for a brief moment until `endElement` will be positioned and we can launch the real transition.
5. `endElement` calls `onElementPositioned`.
6. `transition` is set to `SharedElementTransition.InProgress` and `invalidateTransitionsOverlay()` is called.
7. `SharedElementTransitionsOverlay` reads and draws the real transition from `startElement` to `endElement`

Steps 2, 3 and 4 are done in the same pass (frame?). `endElement` hides itself, but at the same time we already have a placeholder of `startElement` drawn by `SharedElementTransitionsOverlay`.

When going from `UserDetailsScreen` to `UsersListScreen` there is one difference though. I'm not sure if it's a bug of a feature but it looks like `AdapterList` (that is used on `UsersListScreen`) never disposes its list items, even when it's gone. I believe it causes it to be positioned "instantly"(?). The sequence is as follows:

1. `startElement` from previous screen is gone
2. `endElement` calls `onElementRegistered` and hides itself (by using `alpha = 0`)
3. `transition` is set to `SharedElementTransition.WaitingForEndElementPosition` and `invalidateTransitionsOverlay()` is called.
4. **DIFFERENT**: NOT HAPPENING
5. **DIFFERENT**: `endElement` calls `onElementPositioned` in the same pass as 2 and 3. `SharedElementTransitionsOverlay` had no chance to refresh, so we have missed frame.
6. `transition` is set to `SharedElementTransition.InProgress` and `invalidateTransitionsOverlay()` is called.
7. `SharedElementTransitionsOverlay` reads and draws the real transition from `startElement` to `endElement`

### When to remove `SharedElements` from `SharedElementsRootState`?
Currently registered elements are consumed (removed from state) only by a transition animation, when second one from its pair is registered.

Imagine a scenario when:
1. On first screen `FROM` element is registered (and positioned)
2. User navigates to next screen, that has no `SharedElement`
3. User navigates to next screen that has a `TO` element.

Here the transition between `FROM` and `TO` would start because we still remember that `FROM` element from first screen.
I thought it's logical to remove elements from `SharedElementsRootState` when they are disposed (`onDispose`) is called. Although I found (mentioned in previous issue, above) that children of `AdapterList` are actually never disposed, we have no idea that the `FROM` element is gone.

Can it be detected somehow? Is there any way to be notified when something in hierarchy of `SharedElementsRoot` changes? I discovered that we can traverse the hierarchy by casting a `Measurable` to `LayoutNode` in `Layout`'s `MeasureBlock` and then traverse the hierarchy using `visitLayoutChildren`. By doing that we should be able to detect what's actually gone and what's there. Unfortunatelly, I believe that `MeasureBlock` is invoked only when direct children of `Layout` changes, and not for every nested change.

### Types: `FROM` and `TO`
Currently they can be used interchangeably, which means it doesn't matter if first one if `FROM` or `TO`. They are only used to detect the "other" one from the pair.  
Without it I have no idea how to detect that one element was on "previous" screen and second one is on "next" screen, because there is no concept of screens and transitions between them. We cannot rely on things like position or hierarchy change. Not every hierarchy change is a "screen" change and that would make it transition from its old position to its new position (or old/new places in hierarchy).

The names are bit misleading, but it can changed to something more generic like `screen: String` so one can specify values like:
- "list" on one screen
- "details" on second screen

For now I stayed with `FROM` and `TO` because it makes it super simple to choose one o two enum values.
