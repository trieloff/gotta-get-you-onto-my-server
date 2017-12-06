# gotta-get-you-onto-my-server
Provision virtual servers, the serverless way

## Building

```bash
$ npm install
$ npm run release
$ npm run package
```

## Deploy

```bash
$ npm run deploy
```

## Run a Request

```bash
$ source ~/.wskprops && curl https://$APIHOST/api/v1/web/$NAMESPACE/default/echo.json
```