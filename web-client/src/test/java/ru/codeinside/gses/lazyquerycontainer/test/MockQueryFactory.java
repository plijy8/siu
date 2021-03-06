/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */
package ru.codeinside.gses.lazyquerycontainer.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.codeinside.gses.lazyquerycontainer.Query;
import ru.codeinside.gses.lazyquerycontainer.QueryDefinition;
import ru.codeinside.gses.lazyquerycontainer.QueryFactory;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;

public class MockQueryFactory implements QueryFactory {

	private List<Item> items;
	private QueryDefinition definition;
	private int resultSize;
	private int batchQueryMinTime;
	private int batchQueryMaxTime;
	
	public MockQueryFactory(int resultSize, int batchQueryMinTime, int batchQueryMaxTime) {
		this.resultSize=resultSize;
		this.batchQueryMinTime=batchQueryMinTime;
		this.batchQueryMaxTime=batchQueryMaxTime;
	}
	

	public void setQueryDefinition(QueryDefinition definition) {
		this.definition=definition;
	}
	

	public Query constructQuery(Object[] sortPropertyIds, boolean[] ascendingStates) {
		// Creating items on demand when constructQuery is first time called.
		if(items==null) {
			items=new ArrayList<Item>();
			
			for(int i=0;i<resultSize;i++) {
				
				this.items.add(constructItem(items.size(),resultSize-items.size()));
			}
		}
		
		if(sortPropertyIds.length!=0) {
			ItemComparator comparator=new ItemComparator(sortPropertyIds,ascendingStates);
			Collections.sort(this.items,comparator);
		}
		
		return new MockQuery(this,this.items,batchQueryMinTime,batchQueryMaxTime);
	}
	
	public Item constructItem(int indexColumnValue, int reverseIndexColumnValue) {
		// since construct item needs to know what the current size is (including added items)
		// to populate Index and ReverseIndex we should provide it somehow here!
		// At the moment adding multiple items leads to strange behaviour.
		PropertysetItem item=new PropertysetItem();	
		for(Object propertyId : this.definition.getPropertyIds()) {
			
			Object value=null;
			
			if("Index".equals(propertyId)) {
				value=indexColumnValue;
			} else if("ReverseIndex".equals(propertyId)) {
				value=reverseIndexColumnValue;
			} else {
				value=this.definition.getPropertyDefaultValue(propertyId);
			}
			
			item.addItemProperty(propertyId, new ObjectProperty(
					value,
					this.definition.getPropertyType(propertyId),
					this.definition.isPropertyReadOnly(propertyId)
					));
			
		}		
		return item;
	}
	
	public void addProperty(Object propertyId, Class<?> type,
			Object defaultValue, boolean readOnly, boolean sortable) {
		for(Item item : this.items) {
			((PropertysetItem)item).addItemProperty(
					propertyId, new ObjectProperty(defaultValue,type,readOnly));
							
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		private Object[] sortPropertyIds;
		private boolean[] ascendingStates;
		
		public ItemComparator(Object[] sortPropertyIds, boolean[] ascendingStates) {
			this.sortPropertyIds=sortPropertyIds;
			this.ascendingStates=ascendingStates;
		}
		
		public int compare(Item o1, Item o2) {
			
			for(int i=0;i<sortPropertyIds.length;i++) {
				Property p1=o1.getItemProperty(sortPropertyIds[i]);
				Property p2=o2.getItemProperty(sortPropertyIds[i]);
				
				int v1=(Integer)p1.getValue();
				int v2=(Integer)p2.getValue();
				
				if(v1!=v2) {
					int comparison=v1-v2;
					if(!ascendingStates[i]) {
						comparison=-comparison;
					}
					return comparison;
				}
			}
			
			return 0;
		}
		
	}

}
