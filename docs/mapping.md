XSL Mapping
===========

Overview
--------

To create SysAid tickets from Wufoo form submissions, the connector needs to know how to  take the data from a Wufoo form, and use it to populate the fields of one or more tickets. Currently, this mapping is configured based on XSL files that are used to transform an XML document that represents a form entry (entry document) into one that represents one or more SysAid requests (requests document). 

The XML schemas for Entry and Requests documents are described in detail later in this guide. However, on a basic level, an entry document consists of a root `entry` element and a number of children `field` elements, each representing one field from the wufoo form. A requests document consists of a root `requests` element with one or more `request` child elements, which are populated with elements representing the various fields of a SysAid ticket, such as `title` and `category`. Each of these `request` elements will be used to create one SysAid ticket.

Entry XML elements
----------------

###`entry`
The root element of entry xml, this element has two attributes, `hash`, the form hash which is extracted from the path of the request, and `entryID`, an ID which can be used to identify the entry of a given form.

###`field`
An `entry` element will have one or more child `field` elements. These elements have two attributes, `id` and `title`, representing a field's API key and a field's label, respectively. The content of the element is the user's input for that field. For checkboxes, an empty element will represent any unselected fields.

Request XML elements
--------------------
###`requests`
The root element of requests xml, this element will have one or more `request` elements as children. Each of these child elements will represent one SysAid request that will be created when an entry is made for your form.

###`request`
This element will represent a request, and will have a number of child elements that are named below that will represent the various fields of a SysAid request. These elements are listed below

* `title`: The SysAid request title or subject.
* `category`: The category of your ticket, such as Aleph or Web
* `subcategory`: If available for the chosen category, the subcategory.
* `email`: `The e-mail of the request user`
* `firstName1`: First Name of the requst user 
* `lastName`: Last Name of the requst user 
* `description`: Plain-text description of SysAid request
* `usmaiCampus`: 
