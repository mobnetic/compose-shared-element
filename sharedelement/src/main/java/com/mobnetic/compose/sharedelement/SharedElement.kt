package com.mobnetic.compose.sharedelement

import androidx.animation.FloatPropKey
import androidx.animation.TransitionState
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.Recompose
import androidx.compose.invalidate
import androidx.compose.remember
import androidx.compose.staticAmbientOf
import androidx.ui.animation.PxPositionPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.boundsInRoot
import androidx.ui.core.drawLayer
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.layout.Stack
import androidx.ui.layout.offset
import androidx.ui.layout.preferredSize
import androidx.ui.unit.Px
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.height
import androidx.ui.unit.width
import com.mobnetic.compose.sharedelement.SharedElementTransition.InProgress
import com.mobnetic.compose.sharedelement.SharedElementTransition.WaitingForEndElementPosition

enum class SharedElementType { FROM, TO }

@Composable
fun SharedElement(
    tag: Any,
    type: SharedElementType,
    modifier: Modifier = Modifier.None,
    placeholder: @Composable (() -> Unit)? = null,
    children: @Composable() () -> Unit
) {
    val elementInfo = SharedElementInfo(tag, type)

    Recompose { recompose ->
        val elementState = SharedElementsRootStateAmbient.current.getElementState(tag)
        elementState.onElementRegistered(elementInfo)

        Box(
            modifier = modifier.onChildPositioned { coordinates ->
                elementState.onElementPositioned(
                    elementInfo = elementInfo,
                    placeholder = placeholder ?: children,
                    coordinates = coordinates,
                    invalidateElement = recompose
                )
            }.drawLayer(
                alpha = if (elementState.shouldHideElement) 0f else 1f
            ),
            children = children
        )
    }
}

@Composable
fun SharedElementsRoot(children: @Composable() () -> Unit) {
    val rootState = remember { SharedElementsRootState() }

    Stack(modifier = Modifier.onPositioned { layoutCoordinates ->
        rootState.rootCoordinates = layoutCoordinates
    }) {
        Providers(SharedElementsRootStateAmbient provides rootState) {
            children()
        }
        SharedElementTransitionsOverlay(rootState)
    }
}

@Composable
private fun SharedElementTransitionsOverlay(rootState: SharedElementsRootState) {
    rootState.invalidateTransitionsOverlay = invalidate
    rootState.elementStates.values.forEach { elementState ->
        when (val transition = elementState.transition) {
            is WaitingForEndElementPosition -> SharedElementTransitionPlaceholder(
                sharedElement = transition.startElement,
                offsetX = transition.startElement.bounds.left,
                offsetY = transition.startElement.bounds.top
            )
            is InProgress -> Transition(
                definition = transition.transitionDefinition,
                initState = InProgress.State.START,
                toState = InProgress.State.END,
                onStateChangeFinished = { state ->
                    if (state == InProgress.State.END) {
                        transition.onTransitionFinished()
                    }
                }
            ) { transitionState ->
                SharedElementTransitionPlaceholder(
                    sharedElement = transition.startElement,
                    transitionState = transitionState,
                    propKeys = transition.startElementPropKeys
                )
                SharedElementTransitionPlaceholder(
                    sharedElement = transition.endElement,
                    transitionState = transitionState,
                    propKeys = transition.endElementPropKeys
                )
            }
        }
    }
}

@Composable
private fun SharedElementTransitionPlaceholder(sharedElement: PositionedSharedElement, transitionState: TransitionState, propKeys: InProgress.SharedElementPropKeys) {
    SharedElementTransitionPlaceholder(
        sharedElement = sharedElement,
        offsetX = transitionState[propKeys.position].x,
        offsetY = transitionState[propKeys.position].y,
        scaleX = transitionState[propKeys.scaleX],
        scaleY = transitionState[propKeys.scaleY],
        alpha = transitionState[propKeys.alpha]
    )
}

@Composable
private fun SharedElementTransitionPlaceholder(sharedElement: PositionedSharedElement, offsetX: Px, offsetY: Px, scaleX: Float = 1f, scaleY: Float = 1f, alpha: Float = 1f) {
    with(DensityAmbient.current) {
        Box(
            modifier = Modifier.preferredSize(
                width = sharedElement.bounds.width.toDp(),
                height = sharedElement.bounds.height.toDp()
            ).offset(
                x = offsetX.toDp(),
                y = offsetY.toDp()
            ).drawLayer(
//                elevation = Float.MAX_VALUE, // TODO: re-enable in future? Depending on https://issuetracker.google.com/issues/153173354
                scaleX = scaleX,
                scaleY = scaleY,
                alpha = alpha
            ),
            children = sharedElement.placeholder
        )
    }
}

private val SharedElementsRootStateAmbient = staticAmbientOf { SharedElementsRootState() }

private class SharedElementsRootState {
    val elementStates = mutableMapOf<Any, SharedElementState>()
    var invalidateTransitionsOverlay: () -> Unit = {}
    var rootCoordinates: LayoutCoordinates? = null

    fun getElementState(tag: Any): SharedElementState = elementStates.getOrPut(tag) { SharedElementState(this) }

    fun getElementBounds(elementCoordinates: LayoutCoordinates): PxBounds {
        return rootCoordinates?.childBoundingBox(elementCoordinates) ?: elementCoordinates.boundsInRoot
    }
}

private class SharedElementState(
    private val rootState: SharedElementsRootState
) {
    private var startElementInfo: SharedElementInfo? = null
    private var startElement: PositionedSharedElement? = null
    private var endElementInfo: SharedElementInfo? = null
    var transition: SharedElementTransition? = null

    fun onElementRegistered(elementInfo: SharedElementInfo) {
        if (startElementInfo == null) {
            startElementInfo = elementInfo
            return
        } else if (elementInfo == startElementInfo) {
            return
        }

        val startElement = startElement
        if (startElement != null && endElementInfo == null) {
            endElementInfo = elementInfo
            if (transition == null) {
                transition = WaitingForEndElementPosition(startElement)
                rootState.invalidateTransitionsOverlay()
            }
        }
    }

    fun onElementPositioned(
        elementInfo: SharedElementInfo,
        placeholder: @Composable() () -> Unit,
        coordinates: LayoutCoordinates,
        invalidateElement: () -> Unit
    ) {
        val element = PositionedSharedElement(
            info = elementInfo,
            placeholder = placeholder,
            bounds = rootState.getElementBounds(coordinates)
        )

        if (elementInfo == startElementInfo) {
            startElement = element
            return
        }

        val startElement = startElement
        if (startElement != null && elementInfo == endElementInfo) {
            this.startElementInfo = element.info
            this.startElement = element
            this.endElementInfo = null
            if (transition is WaitingForEndElementPosition) {
                transition = InProgress(startElement, element, onTransitionFinished = {
                    transition = null
                    invalidateElement.invoke()
                    rootState.invalidateTransitionsOverlay()
                })
                rootState.invalidateTransitionsOverlay()
            }
        }
    }

    val shouldHideElement: Boolean get() = transition != null
}

private class PositionedSharedElement(
    val info: SharedElementInfo,
    val placeholder: @Composable() () -> Unit,
    val bounds: PxBounds
)

private data class SharedElementInfo(val tag: Any, val type: SharedElementType)

private sealed class SharedElementTransition(val startElement: PositionedSharedElement) {

    class WaitingForEndElementPosition(startElement: PositionedSharedElement) : SharedElementTransition(startElement)

    class InProgress(
        startElement: PositionedSharedElement,
        val endElement: PositionedSharedElement,
        val onTransitionFinished: () -> Unit
    ) : SharedElementTransition(startElement) {

        val startElementPropKeys = SharedElementPropKeys()
        val endElementPropKeys = SharedElementPropKeys()

        val transitionDefinition = transitionDefinition {
            state(State.START) {
                this[startElementPropKeys.position] = PxPosition(startElement.bounds.left, startElement.bounds.top)
                this[startElementPropKeys.scaleX] = 1f
                this[startElementPropKeys.scaleY] = 1f
                this[startElementPropKeys.alpha] = 1f
                this[endElementPropKeys.position] = PxPosition(
                    x = startElement.bounds.left + (startElement.bounds.width - endElement.bounds.width) / 2f,
                    y = startElement.bounds.top + (startElement.bounds.height - endElement.bounds.height) / 2f
                )
                this[endElementPropKeys.scaleX] = startElement.bounds.width / endElement.bounds.width
                this[endElementPropKeys.scaleY] = startElement.bounds.height / endElement.bounds.height
                this[endElementPropKeys.alpha] = 0f
            }
            state(State.END) {
                this[startElementPropKeys.position] = PxPosition(
                    x = endElement.bounds.left + (endElement.bounds.width - startElement.bounds.width) / 2f,
                    y = endElement.bounds.top + (endElement.bounds.height - startElement.bounds.height) / 2f
                )
                this[startElementPropKeys.scaleX] = endElement.bounds.width / startElement.bounds.width
                this[startElementPropKeys.scaleY] = endElement.bounds.height / startElement.bounds.height
                this[startElementPropKeys.alpha] = 0f
                this[endElementPropKeys.position] = PxPosition(endElement.bounds.left, endElement.bounds.top)
                this[endElementPropKeys.scaleX] = 1f
                this[endElementPropKeys.scaleY] = 1f
                this[endElementPropKeys.alpha] = 1f
            }
            transition(fromState = State.START, toState = State.END) {
                sequenceOf(startElementPropKeys, endElementPropKeys).flatMap {
                    sequenceOf(it.position, it.scaleX, it.scaleY, it.alpha)
                }.forEach { key ->
                    key using tween {
                        duration = 300
                    }
                }
            }
        }

        enum class State {
            START, END
        }

        class SharedElementPropKeys {
            val position = PxPositionPropKey()
            val scaleX = FloatPropKey()
            val scaleY = FloatPropKey()
            val alpha = FloatPropKey()
        }
    }
}