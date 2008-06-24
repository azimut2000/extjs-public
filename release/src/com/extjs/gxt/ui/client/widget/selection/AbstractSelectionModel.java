/*
 * Ext GWT - Ext for GWT
 * Copyright(c) 2007, 2008, Ext JS, LLC.
 * licensing@extjs.com
 * 
 * http://extjs.com/license
 */
package com.extjs.gxt.ui.client.widget.selection;

import java.util.ArrayList;
import java.util.List;

import com.extjs.gxt.ui.client.Events;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.event.ContainerEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.Container;
import com.extjs.gxt.ui.client.widget.Items;

/**
 * Concrete selection model. 3 selection models are supported:
 * <ul>
 * <li>Single-select - single select</li>
 * <li>Multi-select - multiple selections using control and shift keys</li>
 * <li>Simple-select - multiple selections without control and shift keys</li>
 * </ul>
 * 
 * @param <C> the container type
 * @param <T> the container item type
 */
public abstract class AbstractSelectionModel<C extends Container<T>, T extends Component>
    implements SelectionModel<C, T>, Listener<ContainerEvent> {

  protected T selectedItem;
  protected List<T> selected;
  protected List<T> selectedPreRender;
  protected C container;
  protected KeyNav keyNav;
  protected SelectionMode mode;
  protected boolean singleSelect, simpleSelect, multiSelect;

  /**
   * Creates a new single-select selection model.
   */
  public AbstractSelectionModel() {
    this(SelectionMode.SINGLE);
  }

  /**
   * Creates a new selection model.
   * 
   * @param mode the selection mode
   */
  public AbstractSelectionModel(SelectionMode mode) {
    this.mode = mode;
    selected = new ArrayList<T>();
    singleSelect = mode == SelectionMode.SINGLE;
    simpleSelect = mode == SelectionMode.SIMPLE;
    multiSelect = mode == SelectionMode.MULTI;
  }

  public void selectAll() {
    if (singleSelect) {
      doSelect(new Items(0), false, false);
    } else {
      doSelect(new Items(0, container.getItemCount()), false, false);
    }
  }

  /**
   * Returns the selection mode.
   * 
   * @return the selection mode
   */
  public SelectionMode getSelectionMode() {
    return mode;
  }

  public void bind(C container) {
    if (keyNav == null) {
      createKeyNav(container);
    }
    if (this.container != null) {
      this.container.removeListener(Events.OnClick, this);
      this.container.removeListener(Events.OnDoubleClick, this);
      this.container.removeListener(Events.Remove, this);
      this.container.removeListener(Events.ContextMenu, this);
      keyNav.bind(null);
    }
    this.container = container;
    if (container != null) {
      container.addListener(Events.OnClick, this);
      container.addListener(Events.OnDoubleClick, this);
      container.addListener(Events.Remove, this);
      container.addListener(Events.ContextMenu, this);
      keyNav.bind(container);
    }
  }

  public void deselect(int index) {
    T item = container.getItem(index);
    if (item != null) {
      deselect(item);
    }
  }

  public void deselect(int start, int end) {
    doDeselect(new Items(start, end));
  }

  public void deselect(List<T> items) {
    doDeselect(new Items(items));
  }

  public void deselect(T... items) {
    doDeselect(new Items(items));
  }

  public void deselect(T item) {
    if (singleSelect) {
      if (selectedItem == item) {
        deselectAll();
      }
    } else {
      doDeselect(new Items(item));
    }
  }

  public void deselectAll() {
    if (singleSelect) {
      if (selectedItem != null) {
        doDeselect(new Items(selectedItem));
      }
    } else {
      deselectAll(false);
    }
  }

  public T getSelectedItem() {
    return selectedItem;
  }

  public List<T> getSelectedItems() {
    return new ArrayList<T>(selected);
  }

  public void handleEvent(ContainerEvent ce) {
    switch (ce.type) {
      case Events.OnClick:
        onClick(ce);
        break;
      case Events.OnDoubleClick:
        onDoubleClick(ce);
        break;
      case Events.ContextMenu:
        onContextMenu(ce);
        break;
      case Events.Remove:
        onRemove(ce);
        break;
    }
  }

  public boolean isSelected(T item) {
    return selected.contains(item);
  }

  public void refresh() {
    for (T item : container.getItems()) {
      onSelectChange(item, selected.contains(item));
    }
  }

  public void select(int index) {
    T item = container.getItem(index);
    if (item != null) {
      select(item);
    }
  }

  public void select(int start, int end) {
    if (singleSelect) {
      doSelect(new Items(start), false, false);
    } else {
      doSelect(new Items(start, end), false, false);
    }
  }

  public void select(List<T> items) {
    if (singleSelect) {
      if (items.size() > 0) {
        select(items.get(0));
      }
    } else {
      doSelect(new Items(items), false, false);
    }
  }

  public void select(T... items) {
    if (singleSelect) {
      if (items.length > 0) {
        select(items[0]);
      }
    } else {
      doSelect(new Items(items), false, false);
    }
  }

  public void select(T item) {
    doSelect(new Items(item), false, false);
  }

  protected void createKeyNav(Container tree) {
    keyNav = new KeyNav<ContainerEvent>() {

      @Override
      public void onDown(ContainerEvent ce) {
        onKeyDown(ce);
      }

      @Override
      public void onLeft(ContainerEvent ce) {
        onKeyLeft(ce);
      }

      @Override
      public void onRight(ContainerEvent ce) {
        onKeyRight(ce);
      }

      @Override
      public void onUp(ContainerEvent ce) {
        onKeyUp(ce);
      }

    };
  }

  protected void deselectAll(boolean supressEvent) {
    for (T item : selected) {
      onSelectChange(item, false);
    }
    selected.clear();
    if (!supressEvent) {
      fireSelectionChanged();
    }
  }

  protected void doDeselect(Items<T> items) {
    boolean change = false;
    for (T item : items.getItems(container)) {
      if (isSelected(item)) {
        change = true;
        onSelectChange(item, false);
        selected.remove(item);
        if (selectedItem == item) {
          selectedItem = null;
        }
      }
    }
    if (change) {
      fireSelectionChanged();
    }
  }

  protected void doMultiSelect(T item, ContainerEvent ce) {
    int index = container.indexOf(item);
    if (ce.isShiftKey() && selectedItem != null) {
      int last = container.indexOf(selectedItem);
      int a = (last > index) ? index : last;
      int b = (last < index) ? index : last;
      doSelect(new Items(a, b + 1), ce.isControlKey(), false);
      selectedItem = container.getItem(last);
    } else {
      if (ce.isControlKey() && isSelected(item)) {
        deselect(container.getItem(index));
      } else {
        doSelect(new Items(index), ce.isControlKey(), false);
      }
    }
  }

  protected void doSelect(final Items<T> items, boolean keepExisting, boolean supressEvent) {
    createContainerEvent(container);
    if (!items.isSingle()) {
      if (!keepExisting) {
        deselectAll();
      }
      for (Object item : items.getItems(container)) {
        doSelect(new Items((Component) item), true, true);
      }
      if (!supressEvent) {
        fireSelectionChanged();
      }
    } else {
      if (!keepExisting) {
        deselectAll(true);
      }
      T item = items.getItem(container);
      if (item != null) {
        onSelectChange(item, true);
        selected.add(item);
        selectedItem = item;
        if (!supressEvent) {
          fireSelectionChanged();
        }
      }
    }
  }

  protected void doSelectChange(T item, boolean select) {
    if (container instanceof Selectable) {
      ((Selectable) container).onSelectChange(item, select);
    }
  }

  protected void doMultiSelect(Items items, boolean keepExisting, boolean supressEvent) {

  }

  protected void doSingleSelect(T item, int index, ContainerEvent ce) {
    if (simpleSelect) {
      if (isSelected(item)) {
        deselect(item);
      } else {
        select(item);
      }
      return;
    }
    if (ce.isControlKey() && isSelected(item)) {
      deselect(container.getItem(index));
    } else {
      doSelect(new Items(item), false, false);
    }
  }

  protected void hookPreRender(T item, boolean select) {
    if (selectedPreRender == null) {
      selectedPreRender = new ArrayList<T>();
      container.addListener(Events.Render, new Listener<ContainerEvent>() {
        public void handleEvent(ContainerEvent be) {
          container.removeListener(Events.Render, this);
          onRender();
        }
      });
    }
    if (select && !selectedPreRender.contains(item)) {
      selectedPreRender.add(item);
    } else if (!select) {
      selectedPreRender.remove(item);
    }
  }

  protected T next() {
    if (selectedItem != null) {
      int index = container.indexOf(selectedItem);
      if (index < (container.getItemCount() - 1)) {
        return container.getItem(++index);
      }
    }
    return null;
  }

  protected void onClick(ContainerEvent ce) {
    T item = (T) ce.item;
    if (item != null) {
      if (simpleSelect) {
        if (isSelected(item)) {
          deselect(item);
        } else {
          doSelect(new Items(item), true, false);
        }
      } else if (singleSelect) {
        doSingleSelect(item, container.indexOf(item), ce);
      } else {
        doMultiSelect(item, ce);
      }
    }
  }

  protected void onContextMenu(ContainerEvent ce) {
    T item = container.findItem(ce.getTarget());
    if (item != null) {
      if (selected.size() > 1 && selected.contains(item)) {
        return;
      }
      select(item);
    }
  }

  protected void onDoubleClick(ContainerEvent ce) {

  }

  protected void onKeyDown(ContainerEvent ce) {
    T item = next();
    if (item != null) {
      doSelect(new Items(item), false, false);
      container.scrollIntoView(item);
      ce.stopEvent();
    }
  }

  protected void onKeyLeft(ContainerEvent ce) {

  }

  protected void onKeyRight(ContainerEvent ce) {

  }

  protected void onKeyUp(ContainerEvent ce) {
    T item = previous();
    if (item != null) {
      doSelect(new Items(item), false, false);
      container.scrollIntoView(item);
      ce.stopEvent();
    }
  }

  protected void onRemove(ContainerEvent ce) {
    T item = (T) ce.item;
    if (isSelected(item)) {
      deselect(item);
    }
  }

  protected void onRender() {
    if (selectedPreRender != null) {
      for (T item : selectedPreRender) {
        doSelectChange(item, true);
      }
      selectedPreRender = null;
    }
  }

  protected void onSelectChange(T item, boolean select) {
    if (!container.isRendered()) {
      hookPreRender(item, select);
      return;
    }
    doSelectChange(item, select);
  }

  protected T previous() {
    if (selectedItem != null) {
      int index = container.indexOf(selectedItem);
      if (index > 0) {
        return container.getItem(--index);
      }
    }
    return null;
  }

  protected void fireSelectionChanged() {
    ContainerEvent event = createContainerEvent(container);
    event.selected = getSelectedItems();
    container.fireEvent(Events.SelectionChange, event);
  }

  protected native ContainerEvent createContainerEvent(Container container) /*-{
   return container.@com.extjs.gxt.ui.client.widget.Container::createContainerEvent(Lcom/extjs/gxt/ui/client/widget/Component;)(null);
   }-*/;

}
