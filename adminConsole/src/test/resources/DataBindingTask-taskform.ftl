<html>
<body>
<head>
<title>DataBindingTest</title>

<script type="text/javascript">

    //add a new row
    function addNewAttachmentInfo(elem){
        var div = elem.parentNode;
        var parentDiv = div.parentNode;
        var arr = div.id.split("__"); //div.id : mode__idNo, e.g. phone__0;
        var mode = arr[0];
        var idNo = parseInt(arr[1]);
        if(shouldAdd(parentDiv)){
            var re = new RegExp("__"+idNo, "g"); //generate the reg exp for __0 or __1 ...
            var newInnerHTML = div.innerHTML.replace(re, "__"+(idNo+1));

            var reg2 = new RegExp("ATTACHMENTS\\[" + idNo + "\\]", 'g');
            newInnerHTML = newInnerHTML.replace(reg2, 'ATTACHMENTS[' + (idNo+1) + ']');

            newDiv = document.createElement("div");
            newDiv.setAttribute("id", mode+"__"+(idNo+1));
            newDiv.innerHTML = newInnerHTML;
            //set each input.value = ""
            var inputs = newDiv.getElementsByTagName("input");
            for(var i=0;i<inputs.length;i++){
                inputs[i].value = "";
            }

            parentDiv.appendChild(newDiv);

            // set the previous delete button visible, set the new added delete button hidden
            // var dele_pre = document.getElementById(mode+"__delete__"+idNo);
            // var dele_new = document.getElementById(mode+"__delete__"+(idNo+1));
            // dele_pre.style.visibility = "visible";
            // dele_new.style.visibility = "hidden";
        }
    }

    //check whether need to add a new row
    function shouldAdd(parentDiv){
        var divs = parentDiv.getElementsByTagName("div"); //get all rows
        var inputs = divs[divs.length-1].getElementsByTagName("input"); //get the last row's inputs
        for(var j=0;j<inputs.length;j++){
            if(inputs[j].value != ""){
                return true; //return true because the last row has values, we should add a new row
            }
        }
        return false;
    }

    //when click the delete button, delete the row
    function deleteAttachmentInfo(currentElement) {
        var parentDiv = currentElement.parentNode.parentNode;
        var t = parentDiv.getElementsByTagName("div").length;
        if (t > 1) {
            parentDiv.removeChild(currentElement.parentNode);
        }
    }
</script>
</head>

<h2>Please input your query data</h2>
<hr>

<form action="complete" method="POST" enctype="multipart/form-data">

<div id="userAction">
<#if USERACTION?exists >
  <B>UserAction:</B> <select name="USERACTION">
  <#if USERACTION == "KEEP">
  <option value="KEEP" selected="selected">KEEP</option>
  <#else>
   <option value="KEEP">KEEP</option>
</#if>
 <#if USERACTION == "REFER">
  <option value="REFER" selected="selected">REFER</option>
  <#else>
   <option value="REFER">REFER</option>
  </#if>
 <#if USERACTION == "ACTION">
  <option value="ACTION" selected="selected">ACTION</option>
  <#else>
   <option value="ACTION">ACTION</option>
  </#if>
   <#if USERACTION == "RELEASE">
  <option value="RELEASE" selected="selected">RELEASE</option>
  <#else>
   <option value="RELEASE">RELEASE</option>
  </#if>
  <#if USERACTION == "DIARY">
  <option value="DIARY" selected="selected">DIARY</option>
  <#else>
   <option value="DIARY">DIARY</option>
  </#if>
  </select>
</#if>
</div>
<br/>

<div id="attachmentInfos">
ATTACHMENTS: 
<div id="attachment__0">
AttachmentID: <input id="attachment__id__0" type="text" name="ATTACHMENTS[0].attachmentID" onkeyup="addNewAttachmentInfo(this);" />
AttachSequence: <input id="attachment__sequence__0" type="text" name="ATTACHMENTS[0].attachSequence" onkeyup="addNewAttachmentInfo(this);" />
AttachStatus: <input id="attachment__status__0" type="text" name="ATTACHMENTS[0].attachStatus" onkeyup="addNewAttachmentInfo(this);" />
<button id="attachment__delete__0" value="Delete" type="button" onclick="deleteAttachmentInfo(this);" />
</div>
</div>

<div id="fieldValues">
FIELDVALUES: 
<div id="fieldvalue__0">
Field Key: <input id="fieldvalue__key__0" type="text" name="FIELDVALUES[0].key" />
Field Value: <input id="fieldvalue__value__0" type="text" name="FIELDVALUES[0].value" />
</div>
<div id="fieldvalue__1">
Field Key: <input id="fieldvalue__key__1" type="text" name="FIELDVALUES[1].key" />
Field Value: <input id="fieldvalue__value__1" type="text" name="FIELDVALUES[1].value" />
</div>
</div>
<br/>

<div id="fieldValues">
</div>
<br/>

<input type="submit" value="Complete">
</form>
</body>
</html>