<h3 id="edittitle" class="rdc-h3">External node information</h3>
Name: <br/>
<input type="text" id="filename" name="filename" style="width: 90%" ><br>
Description:<br/>
<textarea id="description" name="description" rows="3" style="width: 90%" ></textarea><br/>
Link: <br/>
<input type="text" id="link" name="link" style="width: 80%" > <img src="${resource(dir: 'images', file: 'green_check2.png')}"/><br/>
Datatype: <br/>
<select id="datatype" style="width: 92%">
    <!--option disabled>Select data type</option-->
    <g:each in="${types}" var="type">
        <option value="${type.id}">${type.name}</option>
    </g:each>
</select>
