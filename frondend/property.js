// Get selected property from localStorage

let selectedProperty =
    localStorage.getItem("selectedProperty");

if (selectedProperty) {

    document.getElementById("propertyName")
        .innerText = selectedProperty;

}


// Open contact form

function openContactForm() {

    document.getElementById("contactModal")
        .style.display = "block";

}


// Close contact form

function closeContactForm() {

    document.getElementById("contactModal")
        .style.display = "none";

}


// Close modal when clicking outside

window.onclick = function(event) {

    let modal =
        document.getElementById("contactModal");

    if (event.target === modal) {

        modal.style.display = "none";

    }

};


// Submit inquiry

document.getElementById("contactForm")
    .addEventListener("submit", function(event) {

        event.preventDefault();

        let name =
            document.getElementById("name").value;

        let email =
            document.getElementById("contactEmail").value;

        let phone =
            document.getElementById("phone").value;

        let message =
            document.getElementById("formMessage");


        if (name === "" || email === "" || phone === "") {

            message.innerText =
                "Please fill in all required fields.";

            message.style.color = "red";

            return;
        }


        message.innerText =
            "Inquiry sent successfully!";

        message.style.color = "green";


        document.getElementById("contactForm").reset();

    });