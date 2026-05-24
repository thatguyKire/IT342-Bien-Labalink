import { BrowserRouter, Routes, Route, Navigate }
  from 'react-router-dom';
import LoginPage from './features/auth/LoginPage';
import OAuth2CallbackPage from './features/auth/OAuth2CallbackPage';
import MachinePage from './features/machine/MachinePage';
import BookingPage from './features/booking/BookingPage';
import WalletPage from './features/payment/WalletPage';

function ProtectedRoute({
  children
}: {
  children: React.ReactNode
}) {
  const token = localStorage.getItem('token');
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={
          <Navigate to="/login" replace />
        } />
        <Route path="/login"
               element={<LoginPage />} />
        <Route path="/oauth2/callback"
               element={<OAuth2CallbackPage />} />

        {/* Dashboard redirects to machines */}
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <Navigate to="/machines" replace />
          </ProtectedRoute>
        } />

        <Route path="/machines" element={
          <ProtectedRoute>
            <MachinePage />
          </ProtectedRoute>
        } />
        <Route path="/bookings" element={
          <ProtectedRoute>
            <BookingPage />
          </ProtectedRoute>
        } />
        <Route path="/wallet" element={
          <ProtectedRoute>
            <WalletPage />
          </ProtectedRoute>
        } />
      </Routes>
    </BrowserRouter>
  );
}

export default App;