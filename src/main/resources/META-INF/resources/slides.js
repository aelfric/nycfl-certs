(function (slides) {
    let currSlide = location.hash ? Number(location.hash.substr(1)) : 0;

    function switchSlide() {
        console.log({currSlide});
        for (let i = 0; i < slides.length; i++) {
            if (i === currSlide) {
                slides[i].classList.add("active");
            } else {
                slides[i].classList.remove("active");
            }
        }
    }

    function next() {
        currSlide = Math.min(currSlide + 1, slides.length - 1);
        switchSlide();
    }

    function prev() {
        currSlide = Math.max(currSlide - 1, 0);
        switchSlide();
    }

    const btnNext = document.getElementById("btn-next");
    const btnPrev = document.getElementById("btn-prev");

    btnNext.addEventListener('click', next)
    btnPrev.addEventListener('click', prev)

    document.addEventListener('keydown', function (event) {
        if (event.key === 'k' || event.key === 'n') {
            next();
            btnNext.focus();
        }
        if (event.key === 'j' || event.key === 'p') {
            prev();
            btnPrev.focus();
        }
    });
    switchSlide();
    requestAnimationFrame(()=>{
        document.body.classList.add("animated")
    })
})(document.querySelectorAll("section"));
