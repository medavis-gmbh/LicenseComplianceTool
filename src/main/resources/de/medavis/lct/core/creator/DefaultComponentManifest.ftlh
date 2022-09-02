<!DOCTYPE HTML>
<html>
<head>
    <meta charset="utf-8">
    <title>Included third-party software</title>
    <style>
      table#components {
        border-collapse: collapse;
        border-style: hidden;
      }

      table#components, table#components td, table#components th {
        border: 1px solid black;
      }
    </style>
</head>
<body>
<table id="components">
    <thead>
    <tr>
        <th>Component</th>
        <th>Version</th>
        <th>Licenses</th>
    </tr>
    </thead>
    <tbody>
    <#list components as component>
        <tr>
            <td>
                <#if component.url??>
                    <a href="${component.url}">${component.name}</a>
                <#else>
                    ${component.name}
                </#if>
            </td>
            <td>${component.version!}</td>
            <td>
                <#list component.licenses as license>
                    <p>
                        <#if license.url??>
                            <a href="${license.url}">${license.name}</a>
                        <#else>
                            ${license.name}
                        </#if>
                    </p>
                </#list>
            </td>
        </tr>
    </#list>
    </tbody>
</table>
</body>
</html>