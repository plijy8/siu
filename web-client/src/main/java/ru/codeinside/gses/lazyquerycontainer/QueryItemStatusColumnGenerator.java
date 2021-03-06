/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */
package ru.codeinside.gses.lazyquerycontainer;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Property.ValueChangeNotifier;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

/**
 * Helper class for Vaadin tables to generate status column.
 * 
 * @author Tommi S.E. Laukkanen
 */
public final class QueryItemStatusColumnGenerator implements ColumnGenerator, ValueChangeListener {
    /** Serial version UID of this class. */
    private static final long serialVersionUID = 1L;
    /** Icon resource for none state. */
    private Resource noneIconResource;
    /** Icon resource for added state. */
    private Resource addedIconResource;
    /** Icon resource for modified state. */
    private Resource modifiedIconResource;
    /** Icon resource for removed state. */
    private Resource removedIconResource;
    /** The status icon Vaadin component. */
    private Embedded statusIcon;

    /**
     * Construct which sets the application instance.
     */
    public QueryItemStatusColumnGenerator() {
    }

    /**
     * Generates cell component.
     * @param source The table this cell is generated for.
     * @param itemId ID of the item this cell is presenting property of.
     * @param columnId ID of the column this cell is located at.
     * @return Component used to render this cell.
     */
    public Component generateCell(final Table source, final Object itemId, final Object columnId) {
        Property statusProperty = source.getItem(itemId).getItemProperty(columnId);

        noneIconResource = new ClassResource(QueryItemStatusColumnGenerator.class, "images/textfield.png",
                source.getApplication());
        addedIconResource = new ClassResource(QueryItemStatusColumnGenerator.class, "images/textfield_add.png",
                source.getApplication());
        modifiedIconResource = new ClassResource(QueryItemStatusColumnGenerator.class, "images/textfield_rename.png",
                source.getApplication());
        removedIconResource = new ClassResource(QueryItemStatusColumnGenerator.class, "images/textfield_delete.png",
                source.getApplication());

        statusIcon = new Embedded(null, noneIconResource);
        statusIcon.setHeight("16px");

        if (statusProperty instanceof ValueChangeNotifier) {
            ValueChangeNotifier notifier = (ValueChangeNotifier) statusProperty;
            notifier.addListener(this);
        }

        refreshImage(statusProperty);

        return statusIcon;
    }

    /**
     * Event handler for ValueChangeEvent.
     * @param event The event to be handled.
     */
    public void valueChange(final ValueChangeEvent event) {
        refreshImage(event.getProperty());
        statusIcon.requestRepaint();
    }

    /**
     * Refreshes the status Icon according to the property value.
     * @param statusProperty The property according to which status is updated.
     */
    private void refreshImage(final Property statusProperty) {
        if (statusProperty.getValue() == null) {
            statusIcon.setSource(noneIconResource);
            return;
        }
        QueryItemStatus status = (QueryItemStatus) statusProperty.getValue();
        if (status == QueryItemStatus.None) {
            statusIcon.setSource(noneIconResource);
        }
        if (status == QueryItemStatus.Modified) {
            statusIcon.setSource(modifiedIconResource);
        }
        if (status == QueryItemStatus.Added) {
            statusIcon.setSource(addedIconResource);
        }
        if (status == QueryItemStatus.Removed) {
            statusIcon.setSource(removedIconResource);
        }
    }

}
