const noop = () => 0;
export const skikoApi = new Proxy({}, {
get: () => noop,
});
export const awaitSkiko = Promise.resolve({ skikoApi });
export default { skikoApi, awaitSkiko };
