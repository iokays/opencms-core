/*
 * File   : $Source: /alkacon/cvs/opencms/etc/ocsetup/vfs/system/workplace/templates/js/Attic/opencms_edit.js,v $
 * Date   : $Date: 2000/03/08 17:09:37 $
 * Version: $Revision: 1.11 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
 
//------------------------------------------------------//
// Script for editcontrol
//------------------------------------------------------//

// Definition of constants
var CLOSE=1;
var SAVECLOSE=2;
var SAVE=3;

var UNDO=4;
var REDO=5;

var SEARCH=6;
var REPLACE=7;
var GOTO=8;

var CUT=9;
var COPY=10;
var PASTE=11;

var IMPORT=12;
var EXPORT=13;
var EXPORTAS=14;

var PRINT=15;

// Indicates if the text of the editor window is already set
var textSetted = false;


var windowWidth=null;
var windowHeight=null;


// function for calculating the right dimensions of a HTML textarea
function getDimensions() {
	windowWidth = innerWidth - 15;
	windowHeight = innerHeight - space;
	windowWidth = Math.round(windowWidth/8.3);
	windowHeight = Math.round(windowHeight/18.8);
}

// loads the file content into the editor
function setText()
{
   	// setting text can not be done now here for the text editor.
	// MS IE 5 has problems with setting text when the editor control is
	// not loaded. 
	// Workaround: focus() the text editor here and set the text
	// using the onFocus event of the editor.

	document.EDITOR.edit1.focus();
}

// load the file content into the editor. this is called by the onFocus event of the edit control
function setTextDelayed()
{
	if(! textSetted) {
		document.EDITOR.edit1.Text = unescape(text);
		document.EDITOR.edit1.value = unescape(text);
		textSetted = true;
	}
}


function doSubmit()
{
	document.EDITOR.CONTENT.value = escape(document.EDITOR.edit1.Text);
}

// Function action on button click for Netscape Navigator
function doNsEdit(para)
{
	switch(para)
	{
	case 1:
	{
		document.EDITOR.CONTENT.value = escape(document.EDITOR.edit1.value);
		document.EDITOR.action.value = "exit";
		document.EDITOR.submit();
		break;
	}
	case 2:
	{
		document.EDITOR.CONTENT.value = escape(document.EDITOR.edit1.value);
		document.EDITOR.action.value = "saveexit";
		document.EDITOR.submit();
		break;
	}
	case 3:
	{
		document.EDITOR.CONTENT.value = escape(document.EDITOR.edit1.value);
		document.EDITOR.action.value = "save";
		document.EDITOR.submit();
		break;
	}
    }
}

// Function action on button click for MS IE
function doEdit(para)
{
	switch(para)
	{
	case 1:
	{
		doSubmit();
		document.EDITOR.action.value = "exit";
		document.EDITOR.submit();
		break;
	}
	case 2:
	{
		doSubmit();
		document.EDITOR.action.value = "saveexit";
		document.EDITOR.submit();
		break;
	}
	case 3:
	{
		doSubmit();
		document.EDITOR.action.value = "save";
		document.EDITOR.submit();
		break;
	}
	case 4:
	{
		document.all.edit1.Undo();
		break;	
	}
	case 5:
	{
		document.all.edit1.Redo();
		break;	
	}
	case 6:
	{
		document.all.edit1.ShowFindDialog();
		break;
	}
	case 7:
	{
		document.all.edit1.ShowReplaceDialog();
		break;
	}
	case 8:
	{
		document.all.edit1.ShowGotoLineDialog();
		break;	
	}
	case 9:
	{
		document.all.edit1.CutToClipboard();
		break;
	}
	case 10:
	{
		document.all.edit1.CopyToClipboard();
		break;
	}
	case 11:
	{
		document.all.edit1.PasteFromClipboard();
		break;
	}
	case 12:
	{
	    document.all.edit1.OpenFile();
		break;
	}
	case 13:
	{
	    document.all.edit1.SaveFile();
		break;
	}
	case 14:
	{
	    document.all.edit1.SaveFileAs();
		break;
	}
	case 15:
	{
	    document.all.edit1.PrintText();
		break;
	}
	case 16:
	{
		// dummy tag for help
		break;
	}
	default:
	{
		alert("NYI");
		break;
	}
	}	
	document.EDITOR.edit1.focus();
}