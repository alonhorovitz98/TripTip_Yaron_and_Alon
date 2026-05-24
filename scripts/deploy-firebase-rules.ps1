# Deploy Firestore + Storage rules to the shared TripTip Firebase project.
# Run once after: firebase login
Set-Location $PSScriptRoot
firebase use triptip-97085
firebase deploy --only firestore:rules,storage
