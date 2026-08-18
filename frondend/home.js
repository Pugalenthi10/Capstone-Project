function searchProperties() {

    let searchText =
        document.getElementById("searchInput")
        .value
        .toLowerCase();

    let properties =
        document.querySelectorAll(".property-card");

    properties.forEach(function(property) {

        let propertyText =
            property.innerText.toLowerCase();

        if (propertyText.includes(searchText)) {

            property.style.display = "block";

        } else {

            property.style.display = "none";

        }

    });

}


function viewProperty(propertyName) {

    localStorage.setItem(
        "selectedProperty",
        propertyName
    );

    window.location.href = "property.html";
}