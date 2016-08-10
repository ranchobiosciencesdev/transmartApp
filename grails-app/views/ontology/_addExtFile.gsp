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
    <input type="url" id="link" class="widthText" name="link" required /> <!--img id="status" src="${resource(dir: 'images', file: 'green_check2.png')}" style="width: 5%"/--><br/>
    Datatype: <br/>
    <select id="datatype" style="width: 362px">
        <g:each in="${types}" var="type">
            <option value="${type.id}">${type.name}</option>
        </g:each>
    </select>
</form>