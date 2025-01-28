document.addEventListener('DOMContentLoaded', function () {
    const summaryDiv = document.getElementById('result-summary-data-holder');
    if (!summaryDiv) {
    return;
    }

    const cveListingUrl = summaryDiv.getAttribute('data-cve-listing-url');
    const gateOutputUrl = summaryDiv.getAttribute('data-gate-output-url');

    getSummaryRecap('#result-summary', cveListingUrl, gateOutputUrl);
});