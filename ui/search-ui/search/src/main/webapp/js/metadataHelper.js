/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/



/**
 * 
 * This file is being used to come up with ways to display metadata xml in a way
 * that is functional and looks good.
 * 
 */

/* hint for jslint.  Needed due to the recursive use of this function */
/*global buildHtmlFromNode:true */

function convertAttributesToJson(attributes){
	var attr, result;
	result = [];
	for(attr in attributes){
		result.push(attributes[attr].localName + "=" + attributes[attr].value);
	}
	return result;
}

function convertMetadataToJson(metadataString){
	var xmlDoc, result, parent, element, i;
	element = $.parseXML(metadataString);
	result = []; 
	parent = $(xmlDoc)[0].firstChild;
	console.log($(xmlDoc));
	for(i in parent.children){
		element = parent.children[i];
		result.push(
				{	title:element.localName, 
					value:element.textContent, 
					attributes:convertAttributesToJson(element.attributes)
				});
	}
	console.log(result);
	return result;
}


function styleXml(metadataString){
	return "<textarea class='metadata-xml'>"+metadataString+"</textarea>";
}

function styleXmlString(metadataString){
	var formatted = metadataString.replace(/</g,"<<");
	formatted = formatted.replace(/>/g,">>");
	formatted = formatted.replace(/<</g,"<div><span style='color:blue'><</span><span style='color:maroon'>");
	formatted = formatted.replace(/>>/g,"</span><span style='color:blue'>></span></div>");
	
	return "<div class='well'>"+formatted+"</div>";
}

function addAttributes(node, html) {
	if(node.attributes.length) {
		var wrapper, span, iconTag, value, attr, li, i;
		li = $("<li></li>");
		for(i in node.attributes) {
			if($.type(node.attributes[i]) !== "function" && 
					node.attributes[i].name !== undefined) {
				attr = node.attributes[i];
				wrapper = $("<div class=\"input-prepend input-append\"></span> ");
				span = $("<span class=\"add-on\"></span> ");
				iconTag = $("<i class=\"icon-tag\"></i>");
				value = $("<span class=\"add-on attributeValue\"></span> ");
				value.append(document.createTextNode(attr.value));
				span.append(iconTag);
				span.append(document.createTextNode(attr.name));
				wrapper.append(span);
				wrapper.append(value);
				li.append(wrapper);
			}
		}
		html.append(li);
	}
}

function addChildElements(node, html) {
	var child, i;
	for(i in node.childNodes) {
		child = node.childNodes[i];
		if(child.nodeType === 1) {
			html.append(buildHtmlFromNode(child));
		}
	}
}

function addTextElements(node, html) {
	var li, child, i, str = "";
	for(i in node.childNodes) {
		child = node.childNodes[i];
		if(child.nodeType === 3) {
			str += child.textContent + " ";
		}
	}
	if(str.length > 0) {
		li = $("<li></li>");
		li.text(str);
		html.append(li);
	}
}

function buildHtmlFromNode(node) {
	var li, children;
	
	li = $("<li></li>");
	li.append($("<label class=\"tree-toggle nav-header\">" + node.nodeName + "</label>"));
	children = $("<ul class=\"nav nav-list tree\"></ul>");

	addAttributes(node, children);
	addChildElements(node, children);
	addTextElements(node, children);
	
	li.append(children);
	return li;
}

function buildMetadataHtml(xml){
	var xmlDoc, root, html, item;
	xmlDoc = $.parseXML( xml );
	root = xmlDoc.documentElement;
	html = $("<ul class=\"nav nav-list tree\"></ul>");

	item = buildHtmlFromNode(root);
	html.append(item);
	
	return html; 
}
