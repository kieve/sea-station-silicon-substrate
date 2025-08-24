package ca.kieve.ssss.ui.layout;

import ca.kieve.ssss.ui.core.HasUiOrigin;
import ca.kieve.ssss.ui.core.HasUiPosition;
import ca.kieve.ssss.ui.core.HasUiSize;
import ca.kieve.ssss.ui.core.UiParent;
import ca.kieve.ssss.ui.core.UiRenderable;

public interface UiLayout<T> extends
    HasUiSize,
    HasUiPosition,
    HasUiOrigin,
    UiParent<T>,
    UiRenderable
{}
