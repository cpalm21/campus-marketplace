import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyCEN4p92uCCsXK8Sx9NJBvdNMaVVGZKXKs",
  authDomain: "campus-marketplace-3c8c7.firebaseapp.com",
  projectId: "campus-marketplace-3c8c7",
  storageBucket: "campus-marketplace-3c8c7.firebasestorage.app",
  messagingSenderId: "871959594489",
  appId: "1:871959594489:web:a33744d49cd2c80abfe928",
  measurementId: "G-45XY91QXCF"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);

