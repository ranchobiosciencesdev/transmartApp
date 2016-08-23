<style type="text/css">
.widthText {
    width: 360px;
}
input:required:invalid, input:focus:invalid {
    border: 1px solid #b2d1ff;
    background-image: url(${resource(dir: 'images', file: 'uncheck.png')});
    background-position: right top;
    background-repeat: no-repeat;
}
input:required:valid, input:focus:valid {
    border: 1px solid #b2d1ff;
    background-image: url(${resource(dir: 'images', file: 'check.png')});
    background-position: right top;
    background-repeat: no-repeat;
}
textarea:required:invalid, input:focus:invalid {
    border: 1px solid #b2d1ff;
    background-image: url(${resource(dir: 'images', file: 'uncheck.png')});
    background-position: right top;
    background-repeat: no-repeat;
}
textarea:required:valid, input:focus:valid {
    border: 1px solid #b2d1ff;
    background-image: url(${resource(dir: 'images', file: 'check.png')});
    background-position: right top;
    background-repeat: no-repeat;
}
</style>
<h3 id="edittitle" class="rdc-h3">External node information</h3>
<form id="addForm">
    Name: <br/>
    <input id="filename" class="widthText" type="text" name="filename" required /><br>
    Description:<br/>
    <textarea id="description" class="widthText" name="description" rows="3" required ></textarea><br/>
    Link:<br/>
    <input type="url" id="link" class="widthText" name="link" required /><br/>
    Login<br/>
    <input id="link_login" class="widthText" type="text" name="login"/><br>
    Password<br/>
    <input id="link_password" class="widthText" type="password" name="password"/><br>
    Datatype: <br/>
    <select id="datatype" style="width: 362px">
        <g:each in="${types}" var="type">
            <option value="${type.id}">${type.name}</option>
        </g:each>
    </select>
</form>
<br/>
<br/>
<button onclick="saveNewExtFile('${conceptKey}','${conceptid}','${conceptcomment}')">Save</button><button onclick="addwin.hide();showManageExtDialog('${conceptKey}','${conceptid}','${conceptcomment}');">Cancel</button>