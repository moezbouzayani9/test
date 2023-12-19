const IV = CryptoJS.enc.Hex.parse("00000000000000000000000000000000");
const AES_OPTIONS = {
    'iv': IV,
    'padding': CryptoJS.pad.Pkcs7,
    'mode': CryptoJS.mode.CBC,
    'blockSize': 16
};
 
function md5(user) {
    return CryptoJS.enc.Utf8.parse(CryptoJS.MD5(user).toString(CryptoJS.enc.Hex));
}
 
function calculatePinHash(user, pin) {
    const hash = md5(user);
    const encrypted = CryptoJS.AES.encrypt(pin, hash, AES_OPTIONS);
    return encrypted.toString();
}
 
const auth = getAuth();
const user = auth.username;
const pin = '1984';
const pinHash = calculatePinHash(user, pin);
 
pm.variables.set("paymentPinHash", pinHash);
