/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.skin;

import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComboBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIDrawer;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIMenuItem;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIPieMenu;
import com.ardor3d.extension.ui.UIPieMenuItem;
import com.ardor3d.extension.ui.UIPopupMenu;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UIScrollBar;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.UITab;
import com.ardor3d.extension.ui.UITooltip;
import com.ardor3d.extension.ui.text.UIIntegerRollerField;
import com.ardor3d.extension.ui.text.UIPasswordField;
import com.ardor3d.extension.ui.text.UITextArea;
import com.ardor3d.extension.ui.text.UITextField;

public abstract class Skin {

  public void applyTo(final UIComponent component) {
    // NOTE: Test for subclasses first, then parent class

    // 1. BUTTON TYPES
    if (component instanceof UITab) {
      applyToTab((UITab) component);
    } else if (component instanceof UICheckBox) {
      applyToCheckBox((UICheckBox) component);
    } else if (component instanceof UIRadioButton) {
      applyToRadioButton((UIRadioButton) component);
    } else if (component instanceof UIPieMenuItem) {
      applyToPieMenuItem((UIPieMenuItem) component);
    } else if (component instanceof UIMenuItem) {
      applyToMenuItem((UIMenuItem) component);
    } else if (component instanceof UIButton) {
      applyToButton((UIButton) component);
    }

    // 2. OTHER LABEL TYPES
    else if (component instanceof UILabel) {
      applyToLabel((UILabel) component);
    }

    // 3. TEXT ENTRY TYPES
    else if (component instanceof UIPasswordField) {
      applyToPasswordField((UIPasswordField) component);
    } else if (component instanceof UITextField) {
      applyToTextField((UITextField) component);
    } else if (component instanceof UITextArea) {
      applyToTextArea((UITextArea) component);
    } else if (component instanceof UIIntegerRollerField) {
      applyToIntegerRollerField((UIIntegerRollerField) component);
    }

    // 4. PANEL / CONTAINER TYPES
    else if (component instanceof UIProgressBar) {
      applyToProgressBar((UIProgressBar) component);
    } else if (component instanceof UIComboBox) {
      applyToComboBox((UIComboBox) component);
    } else if (component instanceof UIScrollBar) {
      applyToScrollBar((UIScrollBar) component);
    } else if (component instanceof UIPieMenu) {
      applyToPieMenu((UIPieMenu) component);
    } else if (component instanceof UIPopupMenu) {
      applyToPopupMenu((UIPopupMenu) component);
    } else if (component instanceof UIPanel) {
      applyToPanel((UIPanel) component);
    } else if (component instanceof UITooltip) {
      applyToTooltip((UITooltip) component);
    }

    // 5. FRAME TYPES
    else if (component instanceof UIFrame) {
      applyToFrame((UIFrame) component);
    } else if (component instanceof UIDrawer) {
      applyToDrawer((UIDrawer) component);
    }

    // 6. SLIDER
    else if (component instanceof UISlider) {
      applyToSlider((UISlider) component);
    }
  }

  protected abstract void applyToTab(UITab component);

  protected abstract void applyToCheckBox(UICheckBox component);

  protected abstract void applyToRadioButton(UIRadioButton component);

  protected abstract void applyToMenuItem(UIMenuItem component);

  protected abstract void applyToPieMenuItem(UIPieMenuItem component);

  protected abstract void applyToButton(UIButton component);

  protected abstract void applyToLabel(UILabel component);

  protected abstract void applyToTextField(UITextField component);

  protected abstract void applyToPasswordField(UIPasswordField component);

  protected abstract void applyToTextArea(UITextArea component);

  protected abstract void applyToIntegerRollerField(UIIntegerRollerField component);

  protected abstract void applyToPieMenu(UIPieMenu component);

  protected abstract void applyToPanel(UIPanel component);

  protected abstract void applyToTooltip(UITooltip component);

  protected abstract void applyToFrame(UIFrame component);

  protected abstract void applyToDrawer(UIDrawer component);

  protected abstract void applyToProgressBar(UIProgressBar component);

  protected abstract void applyToSlider(UISlider component);

  protected abstract void applyToPopupMenu(UIPopupMenu component);

  protected abstract void applyToComboBox(UIComboBox component);

  protected abstract void applyToScrollBar(UIScrollBar component);
}
