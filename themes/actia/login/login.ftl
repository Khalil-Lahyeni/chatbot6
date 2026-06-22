<#import "template.ftl" as layout>
<@layout.registrationLayout
    displayMessage=!messagesPerField.existsError('username','password')
    displayInfo=false;
    section>

  <#if section = "header"><#-- masqué via CSS --><#elseif section = "form">

    <div class="actia-card">

      <div class="actia-brand">
        <div class="actia-logo-wrap">
          <img src="${url.resourcesPath}/img/logo-actia.png" alt="ACTIA" class="actia-logo" />
        </div>
        <div class="actia-brand-divider"></div>
        <div class="actia-brand-info">
          <span class="actia-app-name">Fleet Management</span>
          <span class="actia-app-sub">Supervision &amp; Monitoring</span>
        </div>
      </div>

      <div class="actia-form-header">
        <h1>Connexion</h1>
        <p>Entrez vos identifiants pour accéder à la plateforme</p>
      </div>

      <#if message?has_content && message.type = 'error'>
        <div class="actia-alert actia-alert-error">
          <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/></svg>
          <span>${kcSanitize(message.summary)?no_esc}</span>
        </div>
      </#if>

      <#if realm.password>
        <form id="kc-form-login" action="${url.loginAction}" method="post" class="actia-form">

          <div class="actia-field ${messagesPerField.existsError('username')?then('actia-field--error', '')}">
            <label for="username">
              <#if !realm.loginWithEmailAllowed>Nom d'utilisateur
              <#elseif !realm.registrationEmailAsUsername>Identifiant ou e-mail
              <#else>Adresse e-mail</#if>
            </label>
            <div class="actia-input-wrap">
              <svg class="actia-input-icon" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10 8a3 3 0 100-6 3 3 0 000 6zM3.465 14.493a1.23 1.23 0 00.41 1.412A9.957 9.957 0 0010 18c2.31 0 4.438-.784 6.131-2.1.43-.333.604-.903.408-1.41a7.002 7.002 0 00-13.074.003z"/></svg>
              <input
                tabindex="1"
                id="username"
                name="username"
                type="text"
                class="actia-input"
                placeholder="votre.identifiant"
                autofocus
                autocomplete="username"
                value="${(login.username)!''}"
              />
            </div>
          </div>

          <div class="actia-field ${messagesPerField.existsError('password')?then('actia-field--error', '')}">
            <label for="password">Mot de passe</label>
            <div class="actia-input-wrap">
              <svg class="actia-input-icon" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd"/></svg>
              <input
                tabindex="2"
                id="password"
                name="password"
                type="password"
                class="actia-input actia-input--password"
                placeholder="••••••••"
                autocomplete="current-password"
              />
              <button type="button" class="actia-toggle-pwd" onclick="togglePassword()" tabindex="-1" aria-label="Afficher/masquer le mot de passe">
                <svg id="icon-eye" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z"/><path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.186A10.004 10.004 0 0110 3c4.257 0 7.893 2.66 9.336 6.41.147.381.146.804 0 1.186A10.004 10.004 0 0110 17c-4.257 0-7.893-2.66-9.336-6.41zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/></svg>
                <svg id="icon-eye-off" style="display:none" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06l-1.745-1.745a10.029 10.029 0 003.3-4.38 1.651 1.651 0 000-1.185A10.004 10.004 0 009.999 3a9.956 9.956 0 00-4.744 1.194L3.28 2.22zM7.752 6.69l1.092 1.092a2.5 2.5 0 013.374 3.373l1.091 1.092a4 4 0 00-5.557-5.557z" clip-rule="evenodd"/><path d="M10.748 13.93l2.523 2.523a10.003 10.003 0 01-8.375-4.854 1.651 1.651 0 010-1.186A10.007 10.007 0 012.839 9.48l1.168 1.168A4 4 0 0010 14a3.989 3.989 0 00.748-.07z"/></svg>
              </button>
            </div>
          </div>

          <div class="actia-options">
            <#if realm.rememberMe && !(usernameEditDisabled??)>
              <label class="actia-checkbox">
                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" <#if (login.rememberMe)??>checked</#if> />
                <span class="actia-checkbox-mark"></span>
                Se souvenir de moi
              </label>
            </#if>
            <#if realm.resetPasswordAllowed>
              <a href="${url.loginResetCredentialsUrl}" class="actia-link" tabindex="5">Mot de passe oublié ?</a>
            </#if>
          </div>

          <#if auth?has_content && auth.showUsername() && !auth.showResetCredentials()>
            <input type="hidden" name="credentialId" value="${(auth.selectedCredential)!''}" />
          </#if>

          <button tabindex="4" id="kc-login" name="login" type="submit" class="actia-btn">
            <span>Se connecter</span>
            <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M3 10a.75.75 0 01.75-.75h10.638L10.23 5.29a.75.75 0 111.04-1.08l5.5 5.25a.75.75 0 010 1.08l-5.5 5.25a.75.75 0 11-1.04-1.08l4.158-3.96H3.75A.75.75 0 013 10z" clip-rule="evenodd"/></svg>
          </button>

        </form>
      </#if>

      <p class="actia-footer">&copy; ${.now?string('yyyy')} ACTIA Group — Fleet Management Platform</p>

    </div>

    <script>
      function togglePassword() {
        var el = document.getElementById('password');
        var isHidden = el.type === 'password';
        el.type = isHidden ? 'text' : 'password';
        document.getElementById('icon-eye').style.display     = isHidden ? 'none' : '';
        document.getElementById('icon-eye-off').style.display = isHidden ? '' : 'none';
      }
    </script>

  </#if>
</@layout.registrationLayout>
