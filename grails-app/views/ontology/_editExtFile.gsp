<style type="text/css">
.widthText {
    width: 360px;
}
/*input:required:invalid, input:focus:invalid {
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
}*/
.input-wrapper{
    position: relative;
}
.input-wrapper input,
.input-wrapper textarea{
    border: 1px solid #b2d1ff;
}
.validation-icon {
    display: block;
    position: absolute;
    top:2px;
    right: 16px;
    z-index: 10;
    width: 16px;
    height: 16px;
    background-image: url(${resource(dir: 'images', file: 'uncheck.png')});
    background-position: right top;
    background-repeat: no-repeat;
}

.validation-icon.valid {
    background-image: url(${resource(dir: 'images', file: 'check.png')});
}
</style>
<h3 id="edittitle" class="rdc-h3">External node information</h3>
<form id="editForm">
    Name: <br/>
    <div class="input-wrapper">
        <input id="filename" class="widthText" type="text" name="filename" required value="${file.name}"/>
        <span class="validation-icon valid"></span>
    </div>

    Description:<br/>
    <div class="input-wrapper">
        <textarea id="description" class="widthText" name="description" rows="3" required >${file.description}</textarea>
        <span class="validation-icon valid"></span>
    </div><br/>
    Link:<br/>
    <div class="input-wrapper">
        <input type="url" id="link" class="widthText" name="link" required value="${file.link}"/>
        <span class="validation-icon valid""></span>
    </div><br/>
    Datatype: <br/>
    <select id="datatype" style="width: 362px">
        <g:each in="${types}" var="type">
            <g:if test="${file.dataType.id==type.id}">
                <option selected value="${type.id}">${type.name}</option>
            </g:if>
            <g:else>
                <option value="${type.id}">${type.name}</option>
            </g:else>
        </g:each>
    </select>
</form>
<br/>
<br/>
<button onclick="saveChangesExtFile('${conceptKey}','${conceptid}','${conceptcomment}',${file.id}, '${file.name}')">Save</button><button onclick="editwin.hide();showManageExtDialog('${conceptKey}','${conceptid}','${conceptcomment}');">Cancel</button>
<script>

    var wrapper = null, i = 0;
    var wrappersCollection = document.getElementsByClassName('input-wrapper');

    while(wrapper = wrappersCollection.item(i++)) {

        wrapper.addEventListener('keyup',function(e) {
            // Find input or textarea and check value to be valid
            // If value is valid add class 'valid' to span, otherwise remove it

            var input = e.target;
            var span = e.target.parentElement.getElementsByTagName("span")[0];
            if(input && span){

                if(input.value.trim() !== '') {
                    if(input.type=='url'){
                        var urlReg = /[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/;
                        var urlRexExp = new RegExp(urlReg);
                        if(urlRexExp.test(input.value.trim())){
                            span.className= "validation-icon valid";
                        }
                    } else {
                        span.className = "validation-icon valid";
                    }
                } else {
                    span.className = "validation-icon";
                }
            }
        });
    }
</script>