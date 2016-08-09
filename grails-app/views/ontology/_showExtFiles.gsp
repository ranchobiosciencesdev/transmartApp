<div>
    <div style="float: right;">
        <span id="closepopup" class="greybutton buttonicon close">Close</span>
    </div>
    <h3 id="edittitle" class="rdc-h3">List of external files</h3>
</div>
<h4><b>Study:</b> ${study}</h4>
<g:if test="${files.isEmpty()}">
    <table class="detail">
        <tr><td>Not found</td></tr>
    </table>
</g:if>
<g:else>
    <table class="extFiles" style="width: 755px;">
        <g:each in="${files}" var="file">
            <tr class="prop">
                <td valign="top" class="name">${file.name} (${file.dataType.name})</td>
                <td valign="top" class="name"><font color="blue">Link: </font><a href=${file.link}>${file.link}</a></td>
                <td class="buttons" rowspan="2"><button class="extButtons" name="">Edit</button>  <button class="extButtons">Delete</button></td>
            </tr>
            <tr class="prop">
                <th colspan="2">
                <font color="gray">${file.description}</font>
                </th>
            </tr>
        </g:each>
    </table>
</g:else>
<!--button id="addButton" name="${study}" class="extButtons" >Add</button-->